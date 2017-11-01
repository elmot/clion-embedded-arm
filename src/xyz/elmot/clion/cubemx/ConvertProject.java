package xyz.elmot.clion.cubemx;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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
    static final String CPROJECT_FILE_NAME = ".cproject";
    private static final String PROJECT_FILE_NAME = ".project";
    private enum STRATEGY { CREATEONLY, OVERWRITE, MERGE/*todo Not supported yet*/}

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
        Context context = new Context(detectAndLoadFile(project, CPROJECT_FILE_NAME));
        Element dotProject = detectAndLoadFile(project, PROJECT_FILE_NAME);
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

        writeProjectFile(project, "tmpl_CMakeLists.txt", "CMakeLists.txt", projectData, STRATEGY.OVERWRITE);
        writeProjectFile(project, "tmpl_gitignore.txt", ".gitignore", projectData,STRATEGY.CREATEONLY);
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
        Messages.showDialog(project,
                projectData.shortHtml(),
                "Project Updated: " + projectData.getProjectName(),
                projectData.extraInfo(),
                new String[]{Messages.OK_BUTTON},0,0, AllIcons.General.InformationDialog);
    }

    private static void writeProjectFile(Project project, String templateName, String fileName, ProjectData projectData, STRATEGY strategy) {

        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> {
                    try {
                        VirtualFile projectFile = project.getBaseDir().findChild(fileName);
                        if(projectFile == null) {
                            projectFile =project.getBaseDir().createChildData(project,fileName);
                        } else  if(strategy == STRATEGY.CREATEONLY) {
                            return;
                        }
                        String template = FileUtil.loadTextAndClose(ConvertProject.class.getResourceAsStream(templateName));
                        String text = new StrSubstitutor(projectData.getAsMap()).replace(template);
                        VfsUtil.saveText(projectFile, text);
                    } catch (IOException e) {
                        application.invokeLater(() ->
                                Messages.showErrorDialog(project, String.format("%s:\n %s ", fileName, e.getLocalizedMessage()), "File Write Error"));
                    }
                }
        );
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
            runManager.addConfiguration(newRunConfig);
            runManager.makeStable(newRunConfig);
            runManager.setSelectedConfiguration(newRunConfig);
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
            Messages.showErrorDialog("File not found",
                    String.format("File %s is not found in the project directory %s", fileName, project.getBasePath()));
            return null;
        }
        try {
            return load(child.getInputStream());
        } catch (IOException | JDOMException e) {
            Messages.showErrorDialog("File Read error",
                    String.format("Failed to read %s in project directory %s\n(%s)", fileName, project.getBasePath(), e.getLocalizedMessage()));
            return null;
        }
    }

    private static class Context {
        final Element cProjectElement;
        String currentKey;

        Context(Element cProjectElement) {
            this.cProjectElement = cProjectElement;
        }
    }
}
