package xyz.elmot.clion.openocd;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

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
        try {
            RunnerAndConfigurationSettings selectedConfiguration = RunManager.getInstance(project).getSelectedConfiguration();
            OpenOcdConfiguration configuration = null;

            if (selectedConfiguration != null && selectedConfiguration.getConfiguration() instanceof OpenOcdConfiguration) {
                configuration = (OpenOcdConfiguration) selectedConfiguration.getConfiguration();
            }

            getOpenOcdComponent(project).startOpenOcd(project, configuration, null, null);
        } catch (ConfigurationException e) {
            Messages.showErrorDialog(project, e.getLocalizedMessage(), e.getTitle());
        }

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
