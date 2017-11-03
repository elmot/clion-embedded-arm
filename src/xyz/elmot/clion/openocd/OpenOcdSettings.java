package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TextAccessor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER;
import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_NONE;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW;

/**
 * (c) elmot on 20.10.2017.
 */
@SuppressWarnings("WeakerAccess")

public class OpenOcdSettings implements ProjectComponent, Configurable {
    public static final String OPENOCD_SCRIPTS_SUBFOLDER = "share/openocd/scripts";
    private final Project project;
    private OpenOcdSettingsPanel panel = null;

    public OpenOcdSettings(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "ARM/OpenOCD support";
    }

    @Override
    public boolean isModified() {
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        return !(Objects.equals(panel.boardConfigFile.getText(), state.boardConfigFile) &&
                Objects.equals(panel.openOcdLocation.getText(), state.openOcdLocation) &&
                Objects.equals(panel.gdbLocation.getText(), state.gdbLocation) &&
                Objects.equals(panel.gdbPort.getValue(), state.gdbPort) &&
                Objects.equals(panel.telnetPort.getValue(), state.telnetPort) &&
                Objects.equals(panel.openOcdScriptsLocation.getText(), state.openOcdScriptsLocation) &&
                Objects.equals(panel.defaultOpenOcdScriptsLocation.isSelected(), state.defaultOpenOcdScriptsLocation));
    }

    @Override
    public void apply() throws ConfigurationException {
        panel.gdbPort.validateContent();
        File projectFile = project.getBasePath() == null ? null : new File(project.getBasePath());
        File ocdFile = checkFileExistenceAndCorrect(projectFile, panel.openOcdLocation, File::canExecute);
        checkFileExistenceAndCorrect(projectFile, panel.gdbLocation, File::canExecute);
        checkFileExistenceAndCorrect(openOcdDefScriptsLocation(ocdFile), panel.boardConfigFile, File::canRead);
        if (!panel.defaultOpenOcdScriptsLocation.isSelected()) {
            checkFileExistenceAndCorrect(ocdFile.getParentFile(), panel.openOcdScriptsLocation, File::isDirectory);
        }
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        state.gdbPort = panel.gdbPort.getValue();
        state.telnetPort = panel.telnetPort.getValue();
        state.boardConfigFile = panel.boardConfigFile.getText();
        state.openOcdLocation = panel.openOcdLocation.getText();
        state.gdbLocation = panel.gdbLocation.getText();
        state.defaultOpenOcdScriptsLocation = panel.defaultOpenOcdScriptsLocation.isSelected();
        state.openOcdScriptsLocation = panel.openOcdScriptsLocation.getText();
    }

    @NotNull
    static File openOcdDefScriptsLocation(File ocdFile) {
        return new File(ocdFile.getParentFile().getParentFile(), OPENOCD_SCRIPTS_SUBFOLDER);
    }

    private File checkFileExistenceAndCorrect(@Nullable File basePath, TextAccessor path, Predicate<File> checkFile) throws ConfigurationException {
        String text = path.getText().trim();
        File file = new File(text);
        if (!file.isAbsolute()) {
            file = new File(basePath, text);
        }
        if (!file.exists()) {
            throw new ConfigurationException("File " + text + " does not exist.");
        }
        if (!checkFile.test(file)) {
            if (!file.canExecute()) {
                throw new ConfigurationException("File " + text + " is invalid");
            }
        } else {
            if (!file.canRead()) {
                throw new ConfigurationException("File " + text + " is not readable");
            }

        }
        if (basePath != null) {
            URI relativized = basePath.toURI().relativize(file.toURI());
            if (!relativized.isAbsolute()) {
                    path.setText(relativized.getPath());
            }
        }
        return file;
    }

    @Override
    public void disposeComponent() {
        panel = null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        panel = new OpenOcdSettingsPanel();
        return panel;
    }

    @Override
    public void reset() {
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        panel.gdbPort.setValue(state.gdbPort);
        panel.telnetPort.setValue(state.telnetPort);
        panel.boardConfigFile.setText(state.boardConfigFile);
        panel.openOcdLocation.setText(state.openOcdLocation);
        panel.openOcdScriptsLocation.setText(state.openOcdScriptsLocation);
        panel.defaultOpenOcdScriptsLocation.setSelected(state.defaultOpenOcdScriptsLocation);
        panel.openOcdScriptsLocation.setEnabled(!state.defaultOpenOcdScriptsLocation);
        panel.gdbLocation.setText(state.gdbLocation);
    }

    /**
     * (c) elmot on 20.10.2017.
     */
    public static class OpenOcdSettingsPanel extends JPanel {

        private final TextFieldWithBrowseButton boardConfigFile;
        private final TextFieldWithBrowseButton openOcdLocation;
        private final TextFieldWithBrowseButton openOcdScriptsLocation;
        private final JBCheckBox defaultOpenOcdScriptsLocation;
        private final TextFieldWithBrowseButton gdbLocation;
        private final IntegerField gdbPort;
        private final IntegerField telnetPort;

        public OpenOcdSettingsPanel() {
            super(new GridLayoutManager(8, 3), true);
            ((GridLayoutManager) getLayout()).setColumnStretch(1, 10);
            openOcdLocation = addFileChooser(0, "OpenOCD Location", null, false, true);

            defaultOpenOcdScriptsLocation = addValueRow(1, "OpenOCD Scripts Default Location", new JBCheckBox("default"));

            openOcdScriptsLocation = addFileChooser(2, "OpenOCD Scripts Location",
                    this::findScriptsLocation, true, false);
            defaultOpenOcdScriptsLocation.addChangeListener(e -> openOcdScriptsLocation.setEnabled(!defaultOpenOcdScriptsLocation.isSelected()));

            boardConfigFile = addFileChooser(3, "Board Config File", this::findBoards, false, false);

            gdbLocation = addFileChooser(4, "Debugger (arm-none-eabi-gdb) Location", null, false, true);


            gdbPort = addValueRow(5, "GDB Port", new IntegerField("GDB Port", 1024, 65353));
            gdbPort.setCanBeEmpty(false);
            telnetPort = addValueRow(6, "Telnet Port", new IntegerField("Telnet Port", 1024, 65353));
            telnetPort.setCanBeEmpty(false);
            add(new Spacer(), new GridConstraints(7, 0, 1, 1, ANCHOR_CENTER, FILL_NONE,
                    SIZEPOLICY_FIXED, SIZEPOLICY_WANT_GROW, null, null, null));
        }

        private File findBoards() {
            if (defaultOpenOcdScriptsLocation.isSelected()) {
                return openOcdDefScriptsLocation(new File(openOcdLocation.getText()));
            } else {
                return new File(openOcdScriptsLocation.getText());
            }
        }

        private File findScriptsLocation() {
            File openOcdLocationFile = new File(openOcdLocation.getText());
            if (!openOcdLocationFile.exists()) return null;
            File defLocation = openOcdDefScriptsLocation(openOcdLocationFile);
            if (defLocation.exists()) return defLocation;
            return openOcdLocationFile.getParentFile();
        }

        private TextFieldWithBrowseButton addFileChooser(int row, @NotNull String labelText, @Nullable Supplier<File> baseSupplier, boolean directory, boolean executable) {
            JBTextField jTextField = new JBTextField();
            ActionListener fileClick = e -> {
                String fileName = jTextField.getText();
                File selected = new File(fileName);
                if (!selected.isAbsolute() && baseSupplier != null) {
                    File base = baseSupplier.get();
                    selected = new File(base, fileName);
                    if (!selected.exists()) {
                        selected = base;
                    }
                }
                FileChooserDescriptor fileDescriptor;
                if (directory) {
                    fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                } else {
                    if (SystemInfo.isWindows && executable) {
                        fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("exe");
                    } else {
                        fileDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
                    }
                }
                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(selected);
                VirtualFile chosenFile = FileChooser.chooseFile(fileDescriptor, null, virtualFile);
                if (chosenFile != null) {
                    jTextField.setText(chosenFile.getCanonicalPath());
                }
            };
            return addValueRow(row, labelText, new TextFieldWithBrowseButton(jTextField, fileClick));
        }

        private <T extends JComponent> T addValueRow(int row, @NotNull String labelText, @NotNull T component) {
            JLabel label = new JLabel(labelText);
            add(label, new GridConstraints(row, 0, 1, 1, ANCHOR_WEST, FILL_NONE,
                    SIZEPOLICY_FIXED, SIZEPOLICY_FIXED, null, null, null));
            add(component, new GridConstraints(row, 1, 1, 1, ANCHOR_WEST,
                    FILL_HORIZONTAL, SIZEPOLICY_WANT_GROW, SIZEPOLICY_FIXED, null, null, null));
            label.setLabelFor(component);
            return component;
        }


    }
}
