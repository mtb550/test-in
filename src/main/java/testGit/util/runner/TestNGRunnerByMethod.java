package testGit.util.runner;

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

import java.util.List;

public class TestNGRunnerByMethod {

    public static void runTestMethod(List<String> fqcn, String methodName) {
        final Project project = Config.getProject();

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    String fqcnString = String.join(".", fqcn);
                    System.out.println("fqcn: " + fqcnString);

                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcnString, GlobalSearchScope.projectScope(project));

                    Module module = null;
                    if (targetClass != null) {
                        module = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    }

                    final Module finalModule = module;

                    ApplicationManager.getApplication().invokeLater(() -> {

                        RunManager runManager = RunManager.getInstance(project);
                        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        String simpleClassName = fqcnString.substring(fqcn.lastIndexOf('.') + 1);
                        String configName = simpleClassName + "." + methodName;

                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(configName);

                        if (settings == null) {
                            settings = runManager.createConfiguration(configName, configType.getConfigurationFactories()[0]);
                            TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

                            configuration.getPersistantData().TEST_OBJECT = TestType.METHOD.getType();
                            configuration.getPersistantData().MAIN_CLASS_NAME = fqcnString;
                            configuration.getPersistantData().METHOD_NAME = methodName;
                            configuration.getPersistantData().getPatterns().clear();

                            if (finalModule != null) {
                                configuration.setModule(finalModule);
                            }

                            runManager.addConfiguration(settings);

                            runManager.setTemporaryConfiguration(settings);
                        }

                        runManager.setSelectedConfiguration(settings);

                        Notification notification = new Notification(
                                "Print",
                                "Starting automated test",
                                "Running: " + configName,
                                NotificationType.INFORMATION
                        );
                        Notifications.Bus.notify(notification, project);

                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    });
                }));
    }
}