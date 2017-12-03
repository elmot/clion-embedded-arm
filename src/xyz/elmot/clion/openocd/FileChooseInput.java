package xyz.elmot.clion.openocd;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.valueEditors.TextFieldValueEditor;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class FileChooseInput extends TextFieldWithBrowseButton {

    protected final TextFieldValueEditor<VirtualFile> editor;
    private final FileChooserDescriptor fileDescriptor;

    protected FileChooseInput(String valueName, VirtualFile defValue) {
        super(new JBTextField());

        editor = new FileTextFieldValueEditor(valueName, defValue);
        fileDescriptor = createFileChooserDescriptor().withFileFilter(this::validateFile);
        installPathCompletion(fileDescriptor);
        addActionListener(e -> {
            VirtualFile virtualFile;
            try {
                virtualFile = parseTextToFile(getTextField().getText());
            } catch (InvalidDataException ignored) {
                virtualFile = getDefaultLocation();
            }
            VirtualFile chosenFile = FileChooser.chooseFile(fileDescriptor, null, virtualFile);
            if (chosenFile != null) {
                getTextField().setText(fileToTextValue(chosenFile));
            }
        });
    }

    protected VirtualFile getDefaultLocation() {
        return VfsUtil.getUserHomeDir();
    }

    protected String fileToTextValue(VirtualFile file) {
        return file.getCanonicalPath();
    }

    protected abstract boolean validateFile(VirtualFile virtualFile);

    protected abstract FileChooserDescriptor createFileChooserDescriptor();

    public String getValueName() {
        return editor.getValueName();
    }

    public void validateContent() throws ConfigurationException {
        editor.validateContent();
    }

    @NotNull
    protected VirtualFile parseTextToFile(@Nullable String text) {
        VirtualFile file = text == null ? editor.getDefaultValue() :
                LocalFileSystem.getInstance().findFileByPath(text);
        if (file == null || !validateFile(file)) {
            throw new InvalidDataException("is invalid");
        }
        return file;
    }

    public static class BoardCfg extends FileChooseInput {

        public static final String SCRIPTS_PATH = "share/openocd/scripts";
        private final Supplier<String> ocdHome;

        public BoardCfg(String valueName, VirtualFile defValue, Supplier<String> ocdHome) {
            super(valueName, defValue);
            this.ocdHome = ocdHome;
        }

        @Override
        protected VirtualFile getDefaultLocation() {
            VirtualFile openOcdHome = getOpenOcdHome();
            if (openOcdHome != null) {
                VirtualFile scriptLocation = openOcdHome.findFileByRelativePath(SCRIPTS_PATH + "/board");
                if (scriptLocation != null) {
                    return scriptLocation;
                }
            }
            return super.getDefaultLocation();
        }

        @NotNull
        @Override
        protected VirtualFile parseTextToFile(@Nullable String text) {
            VirtualFile file;
            if (text == null) {
                file = editor.getDefaultValue();
            } else {
                file = LocalFileSystem.getInstance().findFileByPath(text);
                if (file == null) {
                    VirtualFile openOcdVHome = getOpenOcdHome();
                    if (openOcdVHome != null) {
                        file = openOcdVHome.findFileByRelativePath(SCRIPTS_PATH + "/" + text);
                    }
                }
            }
            if (file == null || !validateFile(file)) {
                throw new InvalidDataException("is invalid");
            }
            return file;
        }

        private VirtualFile getOpenOcdHome() {
            return LocalFileSystem.getInstance().findFileByPath(ocdHome.get());
        }

        @Override
        protected boolean validateFile(VirtualFile virtualFile) {
            return virtualFile.exists() && !virtualFile.isDirectory();
        }

        @Override
        protected FileChooserDescriptor createFileChooserDescriptor() {
            return FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
        }

        @Override
        protected String fileToTextValue(VirtualFile file) {
            String completeFileName = super.fileToTextValue(file);

            VirtualFile ocdHomeVFile = getOpenOcdHome();
            if (ocdHomeVFile != null) {
                VirtualFile ocdScripts = ocdHomeVFile.findFileByRelativePath(SCRIPTS_PATH);
                if (ocdScripts != null) {
                    String relativePath = VfsUtil.getRelativePath(file, ocdScripts);
                    if (relativePath != null) {
                        return relativePath;
                    }
                }
            }
            return completeFileName;
        }
    }

    public static class ExeFile extends FileChooseInput {
        public ExeFile(String valueName, VirtualFile defValue) {
            super(valueName, defValue);
        }

        @Override
        public boolean validateFile(VirtualFile virtualFile) {
            return virtualFile.exists() && !virtualFile.isDirectory()
                    && VfsUtil.virtualToIoFile(virtualFile).canExecute();
        }

        protected FileChooserDescriptor createFileChooserDescriptor() {
            if (SystemInfo.isWindows) {
                return FileChooserDescriptorFactory.createSingleFileDescriptor("exe");
            } else {
                return FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
            }
        }
    }

    public static class OpenOcdHome extends FileChooseInput {
        public OpenOcdHome(String valueName, VirtualFile defValue) {
            super(valueName, defValue);
        }

        @Override
        public boolean validateFile(VirtualFile virtualFile) {
            if (!virtualFile.isDirectory()) return false;
            String binPath = "bin/openocd";
            if (OS.isWindows()) binPath += ".exe";
            VirtualFile openOcdBinary = virtualFile.findFileByRelativePath(binPath);
            if (openOcdBinary == null || openOcdBinary.isDirectory()
                    || !VfsUtil.virtualToIoFile(openOcdBinary).canExecute()) return false;
            VirtualFile scriptsDirectory = virtualFile.findFileByRelativePath("share/openocd/scripts/board");
            return scriptsDirectory != null && scriptsDirectory.isDirectory();
        }

        @Override
        protected FileChooserDescriptor createFileChooserDescriptor() {
            return FileChooserDescriptorFactory.createSingleFolderDescriptor();
        }
    }

    private class FileTextFieldValueEditor extends TextFieldValueEditor<VirtualFile> {
        FileTextFieldValueEditor(String valueName, VirtualFile defValue) {
            super(FileChooseInput.this.getTextField(), valueName, defValue);
        }

        @NotNull
        @Override
        public VirtualFile parseValue(@Nullable String text) {
            return parseTextToFile(text);
        }

        @Override
        public String valueToString(@NotNull VirtualFile value) {
            return value.getPath();
        }

        @Override
        public boolean isValid(@NotNull VirtualFile virtualFile) {
            return FileChooseInput.this.validateFile(virtualFile);
        }
    }
}