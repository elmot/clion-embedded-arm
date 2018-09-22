package xyz.elmot.clion.cubemx;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.OpenOcdSettingsState;

import java.util.List;

/**
 * (c) elmot on 29.10.2017.
 */
public class CubeFileListener implements BaseComponent, BulkFileListener {

    @Override
    public void initComponent() {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null || file.isDirectory()) continue;
            String name = file.getName();
            if (name.endsWith(ConvertProject.CPROJECT_FILE_NAME)) {
                Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
                for (Project openProject : openProjects) {
                    final VirtualFile projectDir = ProjectUtil.guessProjectDir(openProject);
                    if (projectDir!=null && VfsUtilCore.isAncestor(projectDir, file, true)) {
                        ApplicationManager.getApplication().invokeLater(
                                () -> askProjectUpdate(openProject)
                        );
                        return;
                    }
                }

            }
        }
    }

    private void askProjectUpdate(Project openProject) {
        OpenOcdSettingsState ocdSettings = openProject.getComponent(OpenOcdSettingsState.class);
        boolean autoUpdateCmake = ocdSettings != null && ocdSettings.autoUpdateCmake;
        int update = -1;
        if(!autoUpdateCmake) {
            update = Messages.showYesNoDialog(openProject, "Update CMake files?",
                    "STM32CubeMX Project Changed",
                    AllIcons.General.QuestionDialog,
                    new DialogWrapper.DoNotAskOption.Adapter() {
                        @Override
                        public void rememberChoice(boolean isSelected, int exitCode) {
                            if(ocdSettings!=null) {
                                ocdSettings.autoUpdateCmake = isSelected;
                            }
                        }
                    });
        }
        if (autoUpdateCmake || update == Messages.YES) {
            ConvertProject.updateProject(openProject);
        }
    }
}
