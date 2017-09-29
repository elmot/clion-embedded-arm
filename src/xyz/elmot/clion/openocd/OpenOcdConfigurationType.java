package xyz.elmot.clion.openocd;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextIcon;
import org.jetbrains.annotations.NotNull;

/**
 * (c) elmot on 29.9.2017.
 */
public class OpenOcdConfigurationType extends ConfigurationTypeBase {

    private final ConfigurationFactory factory;

    public OpenOcdConfigurationType() {
        super(OpenOcdConfigurationType.class.getName(), "OpenOcd Download & Run",
                "Downloads and Runs Embedded Applications using OpenOcd",
                new TextIcon("OCD", JBColor.ORANGE, JBColor.BLUE, 5));
        factory = new ConfigurationFactory(this) {
            @NotNull
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                RunConfigurationModule runConfigurationModule = new RunConfigurationModule(project);
                return new OpenOcdConfiguration("factory", runConfigurationModule, factory);
            }
        };
        addFactory(factory);
    }

}
