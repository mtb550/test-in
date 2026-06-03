package org.testin.util.runner;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
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
import org.jetbrains.annotations.NotNull;
import org.testin.util.logger.Log;

import java.util.List;

public class TestNGRunnerByMethod {

    public static void runTestMethod(final @NotNull Project project, final @NotNull List<String> rawFqcn) {

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    final String configName = String.join(".", rawFqcn);
                    final String methodName = rawFqcn.getLast();
                    final String classFqcn = String.join(".", rawFqcn.subList(0, rawFqcn.size() - 1));

                    Log.info("[RUNNER] Running Test - configName: " + configName);
                    Log.info("[RUNNER] Extracted  - classFqcn: " + classFqcn + ", methodName: " + methodName);
                    Log.info("[RUNNER] FQCN list size: " + rawFqcn.size() + ", elements: " + rawFqcn);

                    PsiClass targetClass = JavaPsiFacade.getInstance(project).findClass(classFqcn, GlobalSearchScope.projectScope(project));

                    if (targetClass == null) {
                        Log.warn("[RUNNER] Target class not found for FQCN: " + classFqcn);
                        return;
                    }

                    final Module finalModule = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    final String finalFqcn = targetClass.getQualifiedName();
                    final String simpleClassName = classFqcn.substring(classFqcn.lastIndexOf('.') + 1);
                    final String configLabel = simpleClassName + "." + methodName;

                    Log.info("[RUNNER] finalFqcn: " + finalFqcn + ", simpleClass: " + simpleClassName);
                    Log.info("[RUNNER] Config label: " + configLabel);

                    ApplicationManager.getApplication().invokeLater(() -> {

                        final RunManager runManager = RunManager.getInstance(project);
                        final TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(configLabel);
                        final TestNGConfiguration configuration;

                        if (settings == null) {
                            settings = runManager.createConfiguration(configLabel, configType.getConfigurationFactories()[0]);
                            configuration = (TestNGConfiguration) settings.getConfiguration();
                            runManager.addConfiguration(settings);

                        } else
                            configuration = (TestNGConfiguration) settings.getConfiguration();

                        Log.info("[RUNNER] Setting TEST_OBJECT=" + TestType.METHOD.getType() + ", MAIN_CLASS=" + finalFqcn + ", METHOD=" + methodName);

                        configuration.getPersistantData().TEST_OBJECT = TestType.METHOD.getType();
                        configuration.getPersistantData().MAIN_CLASS_NAME = finalFqcn;
                        configuration.getPersistantData().METHOD_NAME = methodName;
                        configuration.getPersistantData().getPatterns().clear();
                        configuration.setAllowRunningInParallel(true);

                        if (finalModule != null)
                            configuration.setModule(finalModule);

                        runManager.setTemporaryConfiguration(settings);
                        runManager.setSelectedConfiguration(settings);
                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    });
                }));
    }

}