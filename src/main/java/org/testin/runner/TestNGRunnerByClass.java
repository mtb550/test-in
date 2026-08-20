package org.testin.runner;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TestNGRunnerByClass {

    public void runTestClass(final @NotNull Project p, final @NotNull String fqcn) {

        if (DumbService.isDumb(p)) {
            DumbService.getInstance(p).showDumbModeNotification(
                    "Cannot run tests while IntelliJ is indexing. Please wait a moment."
            );
            return;
        }

        Logger.info("Running test class: " + fqcn);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApplicationManager.getApplication().runReadAction(() -> {

                    PsiClass targetClass = JavaPsiFacade.getInstance(p)
                            .findClass(fqcn, GlobalSearchScope.projectScope(p));

                    final String finalFqcn = targetClass != null ? targetClass.getQualifiedName() : fqcn;
                    final Module finalModule = targetClass != null ? ModuleUtilCore.findModuleForPsiElement(targetClass) : null;

                    ApplicationManager.getApplication().invokeLater(() -> {

                        if (finalFqcn == null || finalFqcn.isEmpty()) {
                            Logger.warn("Cannot run test class: qualified name is null or empty");
                            return;
                        }

                        RunManager runManager = RunManager.getInstance(p);
                        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        final int dotIndex = finalFqcn.lastIndexOf('.');
                        String simpleClassName = (dotIndex >= 0) ? finalFqcn.substring(dotIndex + 1) : finalFqcn;

                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(simpleClassName);

                        if (settings == null) {
                            settings = runManager.createConfiguration(simpleClassName, configType.getConfigurationFactories()[0]);
                            runManager.addConfiguration(settings);
                        }

                        TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();
                        configuration.getPersistantData().TEST_OBJECT = TestType.CLASS.getType();
                        configuration.getPersistantData().MAIN_CLASS_NAME = finalFqcn;
                        configuration.getPersistantData().getPatterns().clear();

                        if (finalModule != null) {
                            configuration.setModule(finalModule);
                        }

                        runManager.setTemporaryConfiguration(settings);
                        runManager.setSelectedConfiguration(settings);

                        Logger.info("Setting TEST_OBJECT=" + TestType.CLASS.getType() + ", MAIN_CLASS=" + finalFqcn + ", simpleClass=" + simpleClassName);

                        Services.getInstance(p, TestNGExecution.class).launch(settings);
                    });
                });
            } catch (final IndexNotReadyException ex) {
                ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(p).showDumbModeNotification("Indexing interrupted the test run. Please try again."));
            }
        });
    }
}