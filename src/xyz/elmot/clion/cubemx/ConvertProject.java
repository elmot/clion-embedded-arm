package xyz.elmot.clion.cubemx;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.exception.RootRuntimeException;
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType;
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData;
import com.jetbrains.cidr.execution.BuildTargetData;
import com.jetbrains.cidr.execution.ExecutableData;
import org.apache.commons.lang.text.StrSubstitutor;
import org.intellij.lang.annotations.MagicConstant;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter2.AttributeFilter;
import org.jdom.xpath.XPathExpression;
import org.jdom.xpath.XPathFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.openocd.Informational;
import xyz.elmot.clion.openocd.OpenOcdConfiguration;
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

    protected static final String CPROJECT_FILE_NAME = ".cproject";
    private static final String DEFINES_KEY = "gnu.c.compiler.option.preprocessor.def.symbols";
    private static final String CONFIG_DEBUG_XPATH = ".//configuration[@artifactExtension='elf' and @name='Debug']";
    private static final String INCLUDES_KEY = "gnu.c.compiler.option.include.paths";
    private static final String PROJECT_FILE_NAME = ".project";
    private static final String HELP_URL = "https://github.com/elmot/clion-embedded-arm/blob/master/USAGE.md#project-creation-and-conversion-howto";

    public static final AttributeFilter ATTRIBUTES_ONLY = new AttributeFilter();
    private static final XPathExpression<Attribute> INCLUDES_XPATH;
    private static final XPathExpression<Attribute> DEFINES_XPATH;
    private static final XPathExpression<Attribute> SOURCE_XPATH;

    static {
        INCLUDES_XPATH = XPathFactory.instance()
                .compile(CONFIG_DEBUG_XPATH + "//*[@superClass='" + INCLUDES_KEY + "']/listOptionValue/@value",
                        ATTRIBUTES_ONLY);
        DEFINES_XPATH = XPathFactory.instance()
                .compile(CONFIG_DEBUG_XPATH + "//*[@superClass='" + DEFINES_KEY + "']/listOptionValue/@value",
                        ATTRIBUTES_ONLY);
        SOURCE_XPATH = XPathFactory.instance()
                .compile(CONFIG_DEBUG_XPATH + "//sourceEntries/entry/@name", ATTRIBUTES_ONLY);

    }

    @SuppressWarnings("WeakerAccess")
    public ConvertProject() {
        super("Update CMake project with STM32CubeMX project");
    }

    public static void updateProject(Project project) {
        if (project == null) {
            return;
        }
        ProjectData projectData = loadProjectData(project);
        if (projectData == null) {
            return;
        }

        Application application = ApplicationManager.getApplication();
        application.saveAll();
        application.runWriteAction(() -> {
            String fileName = null;
            try {
                final VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
                if (projectDir == null) return;
                fileName = ".gitignore";
                writeProjectFile(project,
                        () -> FileUtil.loadTextAndClose(ConvertProject.class.getResourceAsStream("tmpl_gitignore.txt")),
                        fileName, projectData, STRATEGY.CREATEONLY);

                fileName = "CMakeLists_template.txt";
                VirtualFile cmakeTemplate = projectDir.findOrCreateChildData(project, fileName);
                String templateText;
                if (cmakeTemplate.getLength() <= 0) {
                    templateText = loadCmakeTemplateFromResources();
                    VfsUtil.saveText(cmakeTemplate, templateText);
                } else {
                    templateText = VfsUtil.loadText(cmakeTemplate);
                }
                writeProjectFile(project, () -> templateText, "CMakeLists.txt", projectData, STRATEGY.OVERWRITE);
            } catch (IOException e) {
                String msg = String.format("%s:\n %s ", fileName, e.getLocalizedMessage());
                application.invokeLater(() ->
                        Messages.showErrorDialog(project, msg, "File Write Error"));
            }

        });

        CMakeWorkspace cMakeWorkspace = CMakeWorkspace.getInstance(project);
        if (cMakeWorkspace.getCMakeDependencyFiles().isEmpty()) {
            final VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
            if (projectDir != null)
                cMakeWorkspace.selectProjectDir(VfsUtil.virtualToIoFile(projectDir));
        }
        cMakeWorkspace.scheduleClearGeneratedFilesAndReload();
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                {
                    try {
                        cMakeWorkspace.waitForReloadsToFinish(100000);
                    } catch (TimeoutException e) {
                        throw new RootRuntimeException(e);
                    }
                    modifyCMakeConfigs(project, projectData);

                }
        );

        Informational.showMessage(project, MessageType.INFO,
                "<strong>Project Updated</strong>" + projectData.shortHtml() +
                        "<br>Plugin <a href=\"" + HELP_URL + "\">documentation is located here</a>"
        );
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Nullable
    public static ProjectData loadProjectData(Project project) {
        Element cProject = detectAndLoadFile(project, CPROJECT_FILE_NAME);
        Element dotProject = detectAndLoadFile(project, PROJECT_FILE_NAME);
        if (dotProject == null || cProject == null) {
            return null;
        }
        Context context = new Context(cProject);
        ProjectData projectData = new ProjectData();
        //noinspection ConstantConditions
        projectData.setProjectName(dotProject.getChild("name").getText());
        String linkerScript = fetchValueBySuperClass(context, "fr.ac6.managedbuild.tool.gnu.cross.c.linker.script");
        projectData.setLinkerScript(linkerScript.replace("../", ""));
        projectData.setMcuFamily(fetchValueBySuperClass(context, "fr.ac6.managedbuild.option.gnu.cross.mcu"));
        projectData.setLinkerFlags(fetchValueBySuperClass(context, "gnu.c.link.option.ldflags"));
        projectData.setBoard(fetchValueBySuperClass(context, "fr.ac6.managedbuild.option.gnu.cross.board"));
        projectData.setDefines(loadDefines(context));
        projectData.setIncludes(loadIncludes(context));
        projectData.setSources(loadSources(context));
        return projectData;
    }

    @NotNull
    private static String loadCmakeTemplateFromResources() throws IOException {
        return FileUtil.loadTextAndClose(ConvertProject.class.getResourceAsStream("tmpl_CMakeLists.txt"));
    }

    private static void writeProjectFile(Project project, ThrowableComputable<String, IOException> template,
                                         String fileName, ProjectData projectData, STRATEGY strategy)
            throws IOException {
        final VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) return;
        VirtualFile projectFile = projectDir.findChild(fileName);
        if (projectFile == null) {
            projectFile = projectDir.createChildData(project, fileName);
        } else if (strategy == STRATEGY.CREATEONLY) {
            return;
        }
        String text = new StrSubstitutor(projectData.getAsMap()).replace(template.compute());
        VfsUtil.saveText(projectFile, text);
    }

    private static void modifyCMakeConfigs(Project project, ProjectData projectData) {
        RunManager runManager = RunManager.getInstance(project);
        @SuppressWarnings("ConstantConditions")
        ConfigurationFactory factory =
                ConfigurationTypeUtil.findConfigurationType(OpenOcdConfigurationType.TYPE_ID).getConfigurationFactories()[0];
        String name = "OCD " + projectData.getProjectName();
        if (runManager.findConfigurationByTypeAndName(OpenOcdConfigurationType.TYPE_ID, name) == null) {
            RunnerAndConfigurationSettings newRunConfig = runManager.createConfiguration(name, factory);
            newRunConfig.setShared(true);
            OpenOcdConfiguration configuration = (OpenOcdConfiguration) newRunConfig.getConfiguration();
            configuration.setAllowRunningInParallel(false);
            final CMakeBuildConfigurationHelper helper = CMakeRunConfigurationType.getHelper(project);
            CMakeTarget target = helper.getDefaultTarget();
            if (target != null && !target.isExecutable()) {
                target = helper.getTargets().stream().filter(CMakeTarget::isExecutable).findFirst().orElse(null);
            }
            if (target != null) {
                final BuildTargetData buildTargetData = new BuildTargetData(project.getName(), target.getName());
                final BuildTargetAndConfigurationData data = new BuildTargetAndConfigurationData(buildTargetData, null);
                configuration.setTargetAndConfigurationData(data);
                configuration.setExecutableData(new ExecutableData(buildTargetData));
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                configuration.setBoardConfigFile(SelectBoardDialog.selectBoardByPriority(projectData, project));
                ApplicationManager.getApplication().runWriteAction(() ->
                {
                    runManager.addConfiguration(newRunConfig);
                    runManager.makeStable(newRunConfig);
                    runManager.setSelectedConfiguration(newRunConfig);
                });
            });
        }
    }

    private static String loadIncludes(Context context) {
        List<Attribute> list = INCLUDES_XPATH.evaluate(context.cProjectElement);
        return list.stream()
                .map(Attribute::getValue)
                .map(s -> s.replace("../", ""))
                .collect(Collectors.joining(" "));
    }

    private static String loadSources(Context context) {
        List<Attribute> list = SOURCE_XPATH.evaluate(context.cProjectElement);

        return list.stream()
                .map(Attribute::getValue)
                .map(s -> "\"" + s + "/*.*\"")
                .collect(Collectors.joining(" "));
    }

    private static String loadDefines(Context context) {
        List<Attribute> list = DEFINES_XPATH.evaluate(context.cProjectElement);
        return list.stream()
                .map(Attribute::getValue)
                .map(s -> "-D" + s
                        .replace("\"", "")
                        .replace("(", "\\(")
                        .replace(")", "\\)")
                        .replace("//", "////"))
                .collect(Collectors.joining(" "));
    }

    private static String fetchValueBySuperClass(Context context, @MagicConstant String key) {
        XPathExpression<Attribute> xPath = XPathFactory.instance()
                .compile(".//*[@superClass='" + key + "']/@value", ATTRIBUTES_ONLY);
        return xPath.evaluateFirst(context.cProjectElement).getValue();
    }

    private static Element detectAndLoadFile(Project project, String fileName) {
        final VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        VirtualFile child = projectDir == null ? null : projectDir.findChild(fileName);
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
                    String.format("Failed to read %s in project directory %s\n(%s)", fileName, project.getBasePath(),
                            e.getLocalizedMessage()),
                    "File Read Error");
            return null;
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        Project project = getEventProject(event);
        updateProject(project);
    }

    private enum STRATEGY {CREATEONLY, OVERWRITE}

    private static class Context {
        private final Element cProjectElement;

        Context(Element cProjectElement) {
            this.cProjectElement = cProjectElement;
        }
    }
}
