package org.testin.util.runner;
import org.jetbrains.annotations.NotNull;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.project.Project;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;
import org.testin.util.logger.Logger;

public class TestNGRunnerBySuite {
    public static void runTestSuite(final @NotNull Project p, final String suiteFilePath) {
        /// add notifier here
        if (suiteFilePath == null || suiteFilePath.trim().isEmpty()) {
            Logger.error("Suite file path is invalid.");
            return;
        }

        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();
        RunManager runManager = RunManager.getInstance(p);

        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
                "Run TestNG Suite: " + suiteFilePath, configType.getConfigurationFactories()[0]);
        TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

        configuration.getPersistantData().TEST_OBJECT = TestType.SUITE.getType();
        configuration.getPersistantData().SUITE_NAME = suiteFilePath;

        configuration.getPersistantData().getPatterns().clear();

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }
}