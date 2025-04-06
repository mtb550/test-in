package com.example.Runner;
/*
the optimized method-level TestNG runner using only strings (no PsiClass, no PsiMethod), which runs significantly faster by skipping PSI resolution:

Example:
runTestMethod(project, "com.example.tests.LoginTest", "testInvalidCredentials");

No PSI required = faster and leaner.
*/

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.project.Project;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;

public class TestNGRunnerByMethod {

    public static void runTestMethod(Project project, String fullyQualifiedClassName, String methodName) {
        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();
        RunManager runManager = RunManager.getInstance(project);

        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
                "Run " + methodName + " in " + fullyQualifiedClassName,
                configType.getConfigurationFactories()[0]);
        TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

        // Set as METHOD test object
        configuration.getPersistantData().TEST_OBJECT = TestType.METHOD.getType();
        configuration.getPersistantData().MAIN_CLASS_NAME = fullyQualifiedClassName;
        configuration.getPersistantData().METHOD_NAME = methodName;

        // Clear any existing data
        configuration.getPersistantData().getPatterns().clear();

        // No need to explicitly add to methods collection when setting METHOD_NAME directly
        // The TestNG runner will use MAIN_CLASS_NAME and METHOD_NAME to run the test

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }
}


