package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.CidrExecutableDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * (c) elmot on 29.9.2017.
 */
public class OpenOcdConfiguration extends CMakeAppRunConfiguration implements CidrExecutableDataHolder {


    @SuppressWarnings("WeakerAccess")
    public OpenOcdConfiguration(Project project, ConfigurationFactory configurationFactory, String targetName) {
        super(project, new OpenOcdConfigurationFactoryEx(configurationFactory), targetName);
    }

    @Nullable
    @Override
    public CidrCommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new CidrCommandLineState(environment, new OpenOcdLauncher(this));
    }

    @NotNull
    File findRunFile() throws ExecutionException {
        BuildAndRunConfigurations runConfigurations = getBuildAndRunConfigurations();
        if (runConfigurations == null) {
            throw new ExecutionException("Target is not defined");
        }
        File runFile = runConfigurations.getRunFile();
        if (runFile == null) {
            throw new ExecutionException("Run file is not defined for "+ runConfigurations);
        }
        if (!runFile.exists() || !runFile.isFile()) {
            throw new ExecutionException("Invalid run file "+ runFile.getAbsolutePath());
        }
        return runFile;
    }

    private static class OpenOcdConfigurationFactoryEx extends ConfigurationFactoryEx {
        private final ConfigurationFactory configurationFactory;

        public OpenOcdConfigurationFactoryEx(ConfigurationFactory configurationFactory) {
            super(configurationFactory.getType());
            this.configurationFactory = configurationFactory;
        }

        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return configurationFactory.createTemplateConfiguration(project);
        }

        public String getId() {
            return configurationFactory.getId();
        }

        public void onNewConfigurationCreated(@NotNull CMakeAppRunConfiguration factoryEx) {
            ((ConfigurationFactoryEx<CMakeAppRunConfiguration>) configurationFactory).onNewConfigurationCreated(factoryEx);
        }

        @Override
        public boolean isConfigurationSingletonByDefault() {
            return true;
        }
    }
}
