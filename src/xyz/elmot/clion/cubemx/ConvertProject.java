package xyz.elmot.clion.cubemx;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private static final String DCMAKE_TOOLCHAIN_FILE = "-DCMAKE_TOOLCHAIN_FILE=";

    public ConvertProject() {
        super("Update CMake project with STM32CubeMX project");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = getEventProject(event);
        if (project == null) return;
        Context context = new Context(detectAndLoadFile(project, ".cproject"));
        Element dotProject = detectAndLoadFile(project, ".project");
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

        modifyCMakeConfigs(project, projectData);
        writeProjectFile(project, "tmpl_toolset.txt", projectData.getCmakeFileName(), projectData);
        writeProjectFile(project, "tmpl_CMakeLists.txt", "CMakeLists.txt", projectData);
        writeProjectFile(project, "tmpl_gitignore.txt", ".gitignore", projectData);
        CMakeWorkspace.getInstance(project).scheduleClearGeneratedFilesAndReload();
        Messages.showInfoMessage(project, projectData.toString(), "Project Info");
    }

    @Override
    public boolean startInTransaction() {
        return super.startInTransaction();
    }

    private void writeProjectFile(Project project, String templateName, String fileName, ProjectData projectData) {

        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> {
                    try {
                        VirtualFile childData = project.getBaseDir().findOrCreateChildData(this, fileName);
                        try (OutputStream outputStream = childData.getOutputStream(this);
                             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII)) {
                            String template = FileUtil.loadTextAndClose(ConvertProject.class.getResourceAsStream(templateName));
                            String text = new StrSubstitutor(projectData.getAsMap()).replace(template);
                            writer.write(text);
                        }
                    } catch (IOException e) {
                        application.invokeLater(() ->
                                Messages.showErrorDialog(project, String.format("%s:\n %s ", fileName, e.getLocalizedMessage()), "File Write Error"));
                    }
                }
        );
    }


    private void modifyCMakeConfigs(Project project, ProjectData projectData) {
        CMakeSettings component = project.getComponent(CMakeSettings.class);
        List<CMakeSettings.Configuration> configurations = component.getConfigurations();
        ArrayList<CMakeSettings.Configuration> newConfigs = new ArrayList<>(configurations.size());
        for (CMakeSettings.Configuration configuration : configurations) {
            String options = Objects.toString(configuration.getGenerationOptions(), "");
            options = options.replaceAll(DCMAKE_TOOLCHAIN_FILE + "[^\\s]*", "").trim();
            String newOptions = DCMAKE_TOOLCHAIN_FILE + projectData.getCmakeFileName();
            if (!options.isEmpty()) newOptions += " " + options;
            newConfigs.add(configuration.withGenerationOptions(newOptions));
        }
        component.setConfigurations(newConfigs);
    }

    private String loadIncludes(Context context) throws JDOMException {
        context.currentKey = INCLUDES_KEY;
        @SuppressWarnings("unchecked")
        List<Attribute> list = XPath.selectNodes(context.cProjectElement, INCLUDES_XPATH);
        return list.stream()
                .map(Attribute::getValue)
                .map(s -> s.replace("../", ""))
                .collect(Collectors.joining(" "));
    }

    private String loadSources(Context context) throws JDOMException {
        context.currentKey = SOURCE_XPATH;
        @SuppressWarnings("unchecked")
        List<Attribute> list = XPath.selectNodes(context.cProjectElement, SOURCE_XPATH);

        return list.stream()
                .map(Attribute::getValue)
                .map(s -> "\"" + s + "/*.*\"")
                .collect(Collectors.joining(" "));
    }

    private String loadDefines(Context context) throws JDOMException {
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

    private String fetchValueBySuperClass(Context context, String key) throws JDOMException {
        context.currentKey = key;
        return ((Attribute) XPath.selectSingleNode(context.cProjectElement, ".//*[@superClass='" + key + "']/@value")).getValue();
    }

    private Element detectAndLoadFile(Project project, String fileName) {
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
