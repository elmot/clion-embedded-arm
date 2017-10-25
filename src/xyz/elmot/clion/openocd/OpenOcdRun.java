package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * (c) elmot on 19.10.2017.
 */
public class OpenOcdRun extends AnAction {

    /**
     * Implement this method to provide your action handler.
     *
     * @param event Carries information on the invocation place
     */

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        getOpenOcdComponent(project).startOpenOcd(project, null, null);

    }

    private OpenOcdComponent getOpenOcdComponent(Project project) {
        return project.getComponent(OpenOcdComponent.class);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(!getOpenOcdComponent(e.getProject()).isRun());
        e.getPresentation().setVisible(true);
    }

}
