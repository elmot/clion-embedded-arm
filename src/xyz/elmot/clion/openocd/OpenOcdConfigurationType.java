package xyz.elmot.clion.openocd;

import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * (c) elmot on 29.9.2017.
 */
public class OpenOcdConfigurationType extends CMakeRunConfigurationType {

    private final ConfigurationFactory factory;

    public OpenOcdConfigurationType() {
        //noinspection ConstantConditions
        super("elmot.embedded.openocd.conf.type",
                "EmbeddedApplication",
                "OpenOCD Download & Run",
                "Downloads and Runs Embedded Applications using OpenOCD",
                IconLoader.findIcon("ocd_run.png",OpenOcdConfigurationType.class));
        factory = new ConfigurationFactoryEx(this) {
            @NotNull
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new OpenOcdConfiguration(project, factory, "");
            }

            @Override
            public boolean isConfigurationSingletonByDefault() {
                return true;
            }

        };
    }

    @Override
    public SettingsEditor<? extends CMakeAppRunConfiguration> createEditor(@NotNull Project project) {
        return new CMakeAppRunConfigurationSettingsEditor(project,getHelper(project)) {
            @Override
            protected void createEditorInner(JPanel jPanel, GridBag gridBag) {
                super.createEditorInner(jPanel, gridBag);
                for (Component component : jPanel.getComponents()) {
                    if(component instanceof CommonProgramParametersPanel) {
                        component.setVisible(false);//todo get rid of this hack
                    }
                }
            }
        };
    }

    @NotNull
    @Override
    protected CMakeAppRunConfiguration createRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory configurationFactory) {
//        return new OpenOcdConfiguration(project, factory, ""); // todo uncomment AND fix ids on clion restart
        return new OpenOcdConfiguration(project, configurationFactory, "");
    }
}
