package testGit.Runner;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.project.Project;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;

public class TestNGRunnerByMethod {
    public static void runTestMethod(final Project project, final String fullyQualifiedClassName, final String methodName) {
        // add notifier here
        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();
        RunManager runManager = RunManager.getInstance(project);

        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
                "Run " + methodName + " in " + fullyQualifiedClassName, configType.getConfigurationFactories()[0]);
        TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

        configuration.getPersistantData().TEST_OBJECT = TestType.METHOD.getType();
        configuration.getPersistantData().MAIN_CLASS_NAME = fullyQualifiedClassName;
        configuration.getPersistantData().METHOD_NAME = methodName;

        configuration.getPersistantData().getPatterns().clear();

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }
}