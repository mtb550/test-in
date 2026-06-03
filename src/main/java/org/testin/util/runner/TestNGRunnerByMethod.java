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
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryType;
import org.testin.util.Bundle;
import org.testin.util.Tools;
import org.testin.util.logger.Log;

import java.util.ArrayList;
import java.util.List;

public class TestNGRunnerByMethod {

    public static void runTestMethod(final @NotNull List<String> rawFqcn, final @NotNull String methodName) {
        final Project project = Config.getProject();

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    List<String> cleanedFqcn = sanitizeFqcn(rawFqcn);
                    if (cleanedFqcn.isEmpty()) return;

                    List<String> packageList = new ArrayList<>(cleanedFqcn);
                    String baseClassName = packageList.removeLast();
                    String expectedClassName = Tools.getInstance().toPascalCase(baseClassName);

                    if (expectedClassName.toLowerCase().endsWith("test")) {
                        if (expectedClassName.endsWith("test")) {
                            expectedClassName = expectedClassName.substring(0, expectedClassName.length() - 4) + "Test";
                        }
                    } else {
                        expectedClassName += "Test";
                    }

                    String fqcnString = String.join(".", packageList).toLowerCase() + "." + expectedClassName;
                    Log.info("[RUNNER] Running Test with Cleaned FQCN: " + fqcnString);

                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcnString, GlobalSearchScope.projectScope(project));

                    Module module = null;
                    if (targetClass != null) {
                        module = ModuleUtilCore.findModuleForPsiElement(targetClass);
                    }

                    final Module finalModule = module;
                    final String finalFqcn = fqcnString;

                    String finalExpectedClassName = expectedClassName;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        RunManager runManager = RunManager.getInstance(project);
                        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

                        String configName = finalExpectedClassName + "." + methodName;

                        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(configName);

                        if (settings == null) {
                            settings = runManager.createConfiguration(configName, configType.getConfigurationFactories()[0]);
                            TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

                            configuration.getPersistantData().TEST_OBJECT = TestType.METHOD.getType();
                            configuration.getPersistantData().MAIN_CLASS_NAME = finalFqcn; // الاسم النظيف (azzam.az2.Ts1Test)
                            configuration.getPersistantData().METHOD_NAME = methodName;
                            configuration.getPersistantData().getPatterns().clear();
                            configuration.setAllowRunningInParallel(true);

                            if (finalModule != null)
                                configuration.setModule(finalModule);

                            runManager.addConfiguration(settings);
                        }

                        runManager.setTemporaryConfiguration(settings);
                        runManager.setSelectedConfiguration(settings);
                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    });
                }));
    }

    private static List<String> sanitizeFqcn(List<String> rawFqcn) {
        List<String> sanitized = new ArrayList<>();
        boolean startAdding = false;
        for (String part : rawFqcn) {
            if (startAdding) {
                if (!part.equalsIgnoreCase(DirectoryType.TCD.getDisplayedName())) {
                    sanitized.add(part.replace(" ", "").toLowerCase());
                }
            }
            if (part.equalsIgnoreCase(Bundle.getPluginName())) startAdding = true;
        }
        return sanitized;
    }
}