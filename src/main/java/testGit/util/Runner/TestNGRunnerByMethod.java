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

public class TestNGRunnerByMethod {

    public static void runTestMethod(String fqcn, String methodName) {
        final Project project = Config.getProject();

        // 1. Move PSI lookup to a background thread to prevent UI freezing
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    // 2. Find the class to determine its Module (CRITICAL for the classpath!)
                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcn, GlobalSearchScope.projectScope(project));

                    Module module = null;
                    if (targetClass != null) {
                        module = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    }

                    final Module finalModule = module;

                    // 3. Jump back to the UI thread to create and execute the Run Configuration
                    ApplicationManager.getApplication().invokeLater(() -> {

                        RunManager runManager = RunManager.getInstance(project);
                        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        // Clean name for the IDE Dropdown (e.g., "LoginTest.verifyValidLogin")
                        String simpleClassName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
                        String configName = simpleClassName + "." + methodName;

                        // 4. ENHANCEMENT: Avoid duplicating configurations! Check if it already exists.
                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(configName);

                        if (settings == null) {
                            settings = runManager.createConfiguration(configName, configType.getConfigurationFactories()[0]);
                            TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

                            configuration.getPersistantData().TEST_OBJECT = TestType.METHOD.getType();
                            configuration.getPersistantData().MAIN_CLASS_NAME = fqcn;
                            configuration.getPersistantData().METHOD_NAME = methodName;
                            configuration.getPersistantData().getPatterns().clear();

                            // Attach the module so TestNG has the correct classpath
                            if (finalModule != null) {
                                configuration.setModule(finalModule);
                            }

                            runManager.addConfiguration(settings);

                            // ENHANCEMENT: Make it temporary so it doesn't clutter the user's permanent list
                            runManager.setTemporaryConfiguration(settings);
                        }

                        // Select it in the dropdown
                        runManager.setSelectedConfiguration(settings);

                        // 5. Add the Notifier you requested
                        Notification notification = new Notification(
                                "Print", // This should match a group ID in your plugin.xml if you have one, or just a generic string
                                "Starting automated test",
                                "Running: " + configName,
                                NotificationType.INFORMATION
                        );
                        Notifications.Bus.notify(notification, project);

                        // 6. Execute!
                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    });
                }));
    }
}