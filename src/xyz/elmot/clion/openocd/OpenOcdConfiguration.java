package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * (c) elmot on 29.9.2017.
 */
public class OpenOcdConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>{

    public OpenOcdConfiguration(String name, @NotNull RunConfigurationModule configurationModule, @NotNull ConfigurationFactory factory) {
        super(name, configurationModule, factory);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SettingsEditor<RunConfiguration>() {
            @Override
            protected void resetEditorFrom(@NotNull RunConfiguration s) {

            }

            @Override
            protected void applyEditorTo(@NotNull RunConfiguration s) throws ConfigurationException {

            }

            @NotNull
            @Override
            protected JComponent createEditor() {
                return new JTextField();
            }
        };
    }

    @Override
    public Collection<Module> getValidModules() {
        List<Module> modules = new ArrayList<>();
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project openProject : openProjects) {
            modules.addAll(Arrays.asList(ModuleManager.getInstance(openProject).getModules()));
        }
        return modules;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new RunProfileState() {
            @Nullable
            @Override
            public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
                return null;
            }
        };
    }
}
