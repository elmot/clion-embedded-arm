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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class FileChooseInput extends TextFieldWithBrowseButton {

    public static final String BOARD_FOLDER = "board";
    protected final TextFieldValueEditor<VirtualFile> editor;
    private final FileChooserDescriptor fileDescriptor;

    protected FileChooseInput(String valueName, VirtualFile defValue) {
        super(new JBTextField());

        editor = new FileTextFieldValueEditor(valueName, defValue);
        fileDescriptor = createFileChooserDescriptor().withFileFilter(this::validateFile);
        installPathCompletion(fileDescriptor);
        addActionListener(e -> {
            VirtualFile virtualFile = null;
            String text = getTextField().getText();
            if(text != null && !text.isEmpty())
            try {
                virtualFile = parseTextToFile(text);
            } catch (InvalidDataException ignored) {
                virtualFile = LocalFileSystem.getInstance().findFileByPath(text);
            }
            if(virtualFile == null) {
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

        private final Supplier<String> ocdHome;

        public BoardCfg(String valueName, VirtualFile defValue, Supplier<String> ocdHome) {
            super(valueName, defValue);
            this.ocdHome = ocdHome;
        }

        @Override
        protected VirtualFile getDefaultLocation() {
            VirtualFile ocdScripts = findOcdScripts();
            if (ocdScripts != null) {
                VirtualFile ocdBoards = ocdScripts.findFileByRelativePath(BOARD_FOLDER);
                if (ocdBoards != null) {
                    return ocdBoards;
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
                    VirtualFile ocdScripts = findOcdScripts();
                    if (ocdScripts != null) {
                        file = ocdScripts.findFileByRelativePath(text);
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
            VirtualFile ocdScripts = findOcdScripts();
            if (ocdScripts != null) {
                String relativePath = VfsUtil.getRelativePath(file, ocdScripts);
                if (relativePath != null) {
                    return relativePath;
                }
            }
            return completeFileName;
        }

        @Nullable
        private VirtualFile findOcdScripts() {
            return OpenOcdSettingsState.findOcdScripts(getOpenOcdHome());
        }

    }

    @SuppressWarnings("unused")
    // Might be used in future
    public static class ExeFile extends FileChooseInput {
        @SuppressWarnings("WeakerAccess")
        public ExeFile(String valueName, VirtualFile defValue) {
            super(valueName, defValue);
        }

        @Override
        public boolean validateFile(VirtualFile virtualFile) {
            return virtualFile.exists() && !virtualFile.isDirectory()
                    && VfsUtil.virtualToIoFile(virtualFile).canExecute();
        }

        @Override
        protected FileChooserDescriptor createFileChooserDescriptor() {
            if (SystemInfo.isWindows) {
                return FileChooserDescriptorFactory.createSingleFileDescriptor("exe");
            } else {
                return FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
            }
        }
    }

    public static class OpenOcdHome extends FileChooseInput {
        @SuppressWarnings("WeakerAccess")
        public OpenOcdHome(String valueName, VirtualFile defValue) {
            super(valueName, defValue);
        }

        @Override
        public boolean validateFile(VirtualFile virtualFile) {
            if (!virtualFile.isDirectory()) return false;
            VirtualFile openOcdBinary = virtualFile.findFileByRelativePath(OpenOcdComponent.BIN_OPENOCD);
            if (openOcdBinary == null || openOcdBinary.isDirectory()
                    || !VfsUtil.virtualToIoFile(openOcdBinary).canExecute()) return false;
            VirtualFile ocdScripts = OpenOcdSettingsState.findOcdScripts(virtualFile);
            if(ocdScripts!=null) {
                VirtualFile ocdBoard = ocdScripts.findFileByRelativePath(BOARD_FOLDER);
                return ocdBoard != null && ocdBoard.isDirectory();
            }
            return false;
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