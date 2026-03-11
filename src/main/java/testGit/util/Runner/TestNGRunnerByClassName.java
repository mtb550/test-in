package testGit.util.Runner;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;
import testGit.pojo.Config;

public class TestNGRunnerByClassName {

    public static void runTestClass(String fqcn) {
        final Project project = Config.getProject();

        // 1. Offload heavy PSI lookup to a background thread (Zero UI freezing!)
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    // 2. Resolve the class to find its Module for the classpath
                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcn, GlobalSearchScope.projectScope(project));

                    Module module = null;
                    if (targetClass != null) {
                        module = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    }

                    final Module finalModule = module;

                    // 3. Jump to the UI thread to safely manipulate the RunManager
                    ApplicationManager.getApplication().invokeLater(() -> {

                        RunManager runManager = RunManager.getInstance(project);
                        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        // Create a clean name for the dropdown, e.g., "LoginTest"
                        String simpleClassName = fqcn.substring(fqcn.lastIndexOf('.') + 1);

                        // 4. Check if we already created this configuration to prevent duplicates
                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(simpleClassName);

                        if (settings == null) {
                            settings = runManager.createConfiguration(simpleClassName, configType.getConfigurationFactories()[0]);
                            TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

                            // Tell TestNG to run the entire CLASS
                            configuration.getPersistantData().TEST_OBJECT = TestType.CLASS.getType();
                            configuration.getPersistantData().MAIN_CLASS_NAME = fqcn;
                            configuration.getPersistantData().getPatterns().clear();

                            // Attach module so TestNG can find your dependencies (Selenium, etc.)
                            if (finalModule != null) {
                                configuration.setModule(finalModule);
                            }

                            runManager.addConfiguration(settings);

                            // Make it temporary so IntelliJ auto-cleans it later
                            runManager.setTemporaryConfiguration(settings);
                        }

                        runManager.setSelectedConfiguration(settings);

                        // 5. Add the Notifier
                        Notification notification = new Notification(
                                "Print", // This is your Notification Group ID
                                "Starting test class",
                                "Running: " + simpleClassName,
                                NotificationType.INFORMATION
                        );
                        Notifications.Bus.notify(notification, project);

                        // 6. Execute!
                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    });
                }));
    }
}