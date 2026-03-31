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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;
import testGit.pojo.Config;

public class TestNGRunnerByClass {

    public static void runTestClass(String fqcn) {
        final Project project = Config.getProject();

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).showDumbModeNotification(
                    "Cannot run tests while IntelliJ is indexing. Please wait a moment."
            );
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApplicationManager.getApplication().runReadAction(() -> {

                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcn, GlobalSearchScope.projectScope(project));

                    Module module = null;
                    if (targetClass != null) {
                        module = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    }

                    final Module finalModule = module;

                    ApplicationManager.getApplication().invokeLater(() -> {

                        RunManager runManager = RunManager.getInstance(project);
                        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        String simpleClassName = fqcn.substring(fqcn.lastIndexOf('.') + 1);

                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(simpleClassName);

                        if (settings == null) {
                            settings = runManager.createConfiguration(simpleClassName, configType.getConfigurationFactories()[0]);
                            runManager.addConfiguration(settings);
                            runManager.setTemporaryConfiguration(settings);
                        }

                        TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();
                        configuration.getPersistantData().TEST_OBJECT = TestType.CLASS.getType();
                        configuration.getPersistantData().MAIN_CLASS_NAME = fqcn; // Forces the updated path!
                        configuration.getPersistantData().getPatterns().clear();

                        if (finalModule != null) {
                            configuration.setModule(finalModule);
                        }

                        runManager.setSelectedConfiguration(settings);

                        Notification notification = new Notification(
                                "Print",
                                "Starting test class",
                                "Running: " + simpleClassName,
                                NotificationType.INFORMATION
                        );
                        Notifications.Bus.notify(notification, project);

                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    });
                });
            } catch (IndexNotReadyException e) {
                ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(project).showDumbModeNotification("Indexing interrupted the test run. Please try again."));
            }
        });
    }
}