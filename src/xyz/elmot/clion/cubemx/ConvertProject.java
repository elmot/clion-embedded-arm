package xyz.elmot.clion.cubemx;

import com.intellij.CommonBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.OpenOcdConfigurationType;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.JDOMUtil.load;

/**
 * (c) elmot on 27.9.2017.
 */
public class ConvertProject extends AnAction {

    private static final String DEFINES_KEY = "gnu.c.compiler.option.preprocessor.def.symbols";
    private static final String CONFIG_DEBUG_XPATH = ".//configuration[@artifactExtension='elf' and @name='Debug']";
    private static final String DEFINES_XPATH = CONFIG_DEBUG_XPATH + "//*[@superClass='" + DEFINES_KEY + "']/listOptionValue/@value";
    private static final String INCLUDES_KEY = "gnu.c.compiler.option.include.paths";
    private static final String INCLUDES_XPATH = CONFIG_DEBUG_XPATH + "//*[@superClass='" + INCLUDES_KEY + "']/listOptionValue/@value";
    private static final String SOURCE_XPATH = CONFIG_DEBUG_XPATH + "//sourceEntries/entry/@name";
    protected static final String CPROJECT_FILE_NAME = ".cproject";
    private static final String PROJECT_FILE_NAME = ".project";
    private static final String HELP_URL = "https://github.com/elmot/clion-embedded-arm/blob/master/USAGE.md#project-creation-and-conversion-howto";

    private enum STRATEGY {CREATEONLY, OVERWRITE}

    @SuppressWarnings("WeakerAccess")
    public ConvertProject() {
        super("Update CMake project with STM32CubeMX project");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = getEventProject(event);
        updateProject(project);
    }

    public static void updateProject(Project project) {
        if (project == null) return;
        Element cProject = detectAndLoadFile(project, CPROJECT_FILE_NAME);
        Element dotProject = detectAndLoadFile(project, PROJECT_FILE_NAME);
        if(dotProject == null || cProject == null) {
            return;
        }
        Context context = new Context(cProject);
        ProjectData projectData = new ProjectData();
        //noinspection ConstantConditions
        projectData.setProjectName(dotProject.getChild("name").getText());
        try {
            String linkerScript = fetchValueBySuperClass(context, "fr.ac6.managedbuild.tool.gnu.cross.c.linker.script");
            projectData.setLinkerScript(linkerScript.replace("../", ""));
            projectData.setMcuFamily(fetchValueBySuperClass(context, "fr.ac6.managedbuild.option.gnu.cross.mcu"));
            projectData.setLinkerFlags(fetchValueBySuperClass(context, "gnu.c.link.option.ldflags"));

            projectData.setDefines(loadDefines(context));
            projectData.setIncludes(loadIncludes(context));
            projectData.setSources(loadSources(context));
        } catch (JDOMException e) {
            Messages.showErrorDialog(project, "Xml data error", String.format("XML data retrieval error\n Key: %s ", context.currentKey));
        }

        Application application = ApplicationManager.getApplication();
        application.saveAll();
        application.runWriteAction(() -> {
            String fileName = null;
            try {
                fileName = ".gitignore";
                writeProjectFile(project, () -> FileUtil.loadTextAndClose(ConvertProject.class.getResourceAsStream("tmpl_gitignore.txt")), fileName, projectData, STRATEGY.CREATEONLY);

                fileName = "CMakeLists_template.txt";
                VirtualFile cmakeTemplate = project.getBaseDir().findOrCreateChildData(project, fileName);
                String templateText;
                if (cmakeTemplate.getLength() <= 0) {
                    templateText = loadCmakeTemplateFromResources();
                    VfsUtil.saveText(cmakeTemplate, templateText);
                } else {
                    templateText = VfsUtil.loadText(cmakeTemplate);
                }
                fileName = "CMakeLists.txt";
                writeProjectFile(project, () -> templateText, fileName, projectData, STRATEGY.OVERWRITE);
            } catch (IOException e) {
                String msg = String.format("%s:\n %s ", fileName, e.getLocalizedMessage());
                application.invokeLater(() ->
                        Messages.showErrorDialog(project, msg, "File Write Error"));
            }

        });

        CMakeWorkspace cMakeWorkspace = CMakeWorkspace.getInstance(project);
        cMakeWorkspace.scheduleClearGeneratedFilesAndReload();
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                {
                    try {
                        cMakeWorkspace.waitForReloadsToFinish();
                    } catch (TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                    modifyCMakeConfigs(project, projectData);

                }
        );
        if (Messages.showDialog(project, projectData.shortHtml(), "Project Updated",
                new String[]{Messages.OK_BUTTON, CommonBundle.getHelpButtonText()}, 0,
                AllIcons.General.InformationDialog) == 1) {
            BrowserUtil.browse(HELP_URL);
        }
    }

    @NotNull
    private static String loadCmakeTemplateFromResources() throws IOException {
        return FileUtil.loadTextAndClose(ConvertProject.class.getResourceAsStream("tmpl_CMakeLists.txt"));
    }

    private static void writeProjectFile(Project project, ThrowableComputable<String, IOException> template,
                                         String fileName, ProjectData projectData, STRATEGY strategy) throws IOException {
        VirtualFile projectFile = project.getBaseDir().findChild(fileName);
        if (projectFile == null) {
            projectFile = project.getBaseDir().createChildData(project, fileName);
        } else if (strategy == STRATEGY.CREATEONLY) {
            return;
        }
        String text = new StrSubstitutor(projectData.getAsMap()).replace(template.compute());
        VfsUtil.saveText(projectFile, text);
    }

    private static void modifyCMakeConfigs(Project project, ProjectData projectData) {
        RunManager runManager = RunManager.getInstance(project);
        @SuppressWarnings("ConstantConditions")
        ConfigurationFactory factory = runManager.getConfigurationType(OpenOcdConfigurationType.TYPE_ID)
                .getConfigurationFactories()[0];
        String name = "OCD " + projectData.getProjectName();
        if (runManager.findConfigurationByTypeAndName(OpenOcdConfigurationType.TYPE_ID, name) == null) {
            RunnerAndConfigurationSettings newRunConfig = runManager.createRunConfiguration(name, factory);
            newRunConfig.setSingleton(true);
            ((CMakeAppRunConfiguration) newRunConfig.getConfiguration()).setupDefaultTargetAndExecutable();
            ApplicationManager.getApplication().invokeLater(() ->
                    ApplicationManager.getApplication().runWriteAction(() ->
                    {
                        runManager.addConfiguration(newRunConfig);
                        runManager.makeStable(newRunConfig);
                        runManager.setSelectedConfiguration(newRunConfig);
                    }));
        }
    }

    private static String loadIncludes(Context context) throws JDOMException {
        context.currentKey = INCLUDES_KEY;
        @SuppressWarnings("unchecked")
        List<Attribute> list = XPath.selectNodes(context.cProjectElement, INCLUDES_XPATH);
        return list.stream()
                .map(Attribute::getValue)
                .map(s -> s.replace("../", ""))
                .collect(Collectors.joining(" "));
    }

    private static String loadSources(Context context) throws JDOMException {
        context.currentKey = SOURCE_XPATH;
        @SuppressWarnings("unchecked")
        List<Attribute> list = XPath.selectNodes(context.cProjectElement, SOURCE_XPATH);

        return list.stream()
                .map(Attribute::getValue)
                .map(s -> "\"" + s + "/*.*\"")
                .collect(Collectors.joining(" "));
    }

    private static String loadDefines(Context context) throws JDOMException {
        context.currentKey = DEFINES_KEY;
        @SuppressWarnings("unchecked")
        List<Attribute> list = XPath.selectNodes(context.cProjectElement, DEFINES_XPATH);
        return list.stream()
                .map(Attribute::getValue)
                .map(s -> "-D" + s
                        .replace("\"", "")
                        .replace("(", "\\(")
                        .replace(")", "\\)")
                        .replace("//", "////"))
                .collect(Collectors.joining(" "));
    }

    private static String fetchValueBySuperClass(Context context, String key) throws JDOMException {
        context.currentKey = key;
        return ((Attribute) XPath.selectSingleNode(context.cProjectElement, ".//*[@superClass='" + key + "']/@value")).getValue();
    }

    private static Element detectAndLoadFile(Project project, String fileName) {
        VirtualFile child = project.getBaseDir().findChild(fileName);
        if (child == null || !child.exists() || child.isDirectory()) {
            Messages.showErrorDialog(
                    String.format("File %s is not found in the project directory %s", fileName, project.getBasePath()),
                    "File not Found");
            return null;
        }
        try {
            return load(child.getInputStream());
        } catch (IOException | JDOMException e) {
            Messages.showErrorDialog(
                    String.format("Failed to read %s in project directory %s\n(%s)", fileName, project.getBasePath(), e.getLocalizedMessage()),
                    "File Read Error");
            return null;
        }
    }

    private static class Context {
        private final Element cProjectElement;
        private String currentKey;

        Context(Element cProjectElement) {
            this.cProjectElement = cProjectElement;
        }
    }
}
