package org.testin.util.runner;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
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
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.util.ArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TestNGRunnerByMethod {

    public void runTestMethod(final @NotNull Project project, final @NotNull TestCaseDto tc) {
        ArrayList<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnMethod(tc);

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    final String configName = String.join(".", fqcn);
                    final String methodName = fqcn.getLast();
                    final String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));

                    Log.info("Running Test - configName: " + configName);
                    Log.info("Extracted  - classFqcn: " + classFqcn + ", methodName: " + methodName);
                    Log.info("FQCN list size: " + fqcn.size() + ", elements: " + fqcn);

                    PsiClass targetClass = JavaPsiFacade.getInstance(project).findClass(classFqcn, GlobalSearchScope.projectScope(project));

                    if (targetClass == null) {
                        Log.warn("Target class not found for FQCN: " + classFqcn);
                        return;
                    }

                    final Module finalModule = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    final String finalFqcn = targetClass.getQualifiedName();
                    final int dotIndex = classFqcn.lastIndexOf('.');
                    final String simpleClassName = (dotIndex >= 0) ? classFqcn.substring(dotIndex + 1) : classFqcn;
                    final String configLabel = simpleClassName + "." + methodName;

                    Log.info("finalFqcn: " + finalFqcn + ", simpleClass: " + simpleClassName);
                    Log.info("Config label: " + configLabel);

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

                        Log.info("Setting TEST_OBJECT=" + TestType.METHOD.getType() + ", MAIN_CLASS=" + finalFqcn + ", METHOD=" + methodName);

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