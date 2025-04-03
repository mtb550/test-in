package com.example.Runner;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;

public class TestNGRunner {
    // Existing method to run all tests in a class by class name
    public static void runTestClass(Project project, String className) {
        PsiClass psiClass = ReadAction.compute(() ->
                JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
        );

        if (psiClass == null) {
            throw new RuntimeException("Test class not found: " + className);
        }

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = runManager.createConfiguration("Run " + className,
                TestNGConfigurationType.getInstance().getConfigurationFactories()[0]);
        TestNGConfiguration config = (TestNGConfiguration) settings.getConfiguration();

        ReadAction.run(() -> config.beClassConfiguration(psiClass));

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    // Existing method to debug all tests in a class by class name
    public static void debugTestClass(Project project, String className) {
        PsiClass psiClass = ReadAction.compute(() ->
                JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
        );

        if (psiClass == null) {
            throw new RuntimeException("Test class not found: " + className);
        }

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = runManager.createConfiguration("Run " + className,
                TestNGConfigurationType.getInstance().getConfigurationFactories()[0]);
        TestNGConfiguration config = (TestNGConfiguration) settings.getConfiguration();

        ReadAction.run(() -> config.beClassConfiguration(psiClass));

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        ProgramRunnerUtil.executeConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance());
    }

    // New method to run a specific TestNG test method given "com.example.MyTest.testMethod"
    public static void runTestMethod(Project project, String classAndMethod) {
        int dotIndex = classAndMethod.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("Expected format <ClassName>.<methodName>, got: " + classAndMethod);
        }
        String className = classAndMethod.substring(0, dotIndex);
        String methodName = classAndMethod.substring(dotIndex + 1);
        if (className.isEmpty() || methodName.isEmpty()) {
            throw new IllegalArgumentException("Invalid class or method name in: " + classAndMethod);
        }
        ReadAction.nonBlocking(() -> {
                    // Locate the PsiClass and PsiMethod in a read action (background thread)
                    PsiClass psiClass = JavaPsiFacade.getInstance(project)
                            .findClass(className, GlobalSearchScope.allScope(project));
                    if (psiClass == null) {
                        throw new RuntimeException("Test class not found: " + className);
                    }
                    PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
                    if (methods.length == 0) {
                        throw new RuntimeException("Test method not found: " + methodName + " in class " + className);
                    }
                    return methods[0];
                }).expireWhen(project::isDisposed)
                .finishOnUiThread(ModalityState.NON_MODAL, psiMethod -> {
                    // Create and configure TestNG run configuration on the UI thread
                    RunManager runManager = RunManager.getInstance(project);
                    String configName = className + "." + methodName;
                    RunnerAndConfigurationSettings settings = runManager.createConfiguration(configName, TestNGConfigurationType.class);
                    TestNGConfiguration config = (TestNGConfiguration) settings.getConfiguration();
                    // Set up configuration to run the specific method
                    ApplicationManager.getApplication().runReadAction(() ->
                            config.beMethodConfiguration(new PsiLocation<>(project, psiMethod))
                    );
                    runManager.addConfiguration(settings);
                    ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                }).submit(AppExecutorUtil.getAppExecutorService());
    }

    // New method to debug a specific TestNG test method given "com.example.MyTest.testMethod"
    public static void debugTestMethod(Project project, String classAndMethod) {
        int dotIndex = classAndMethod.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("Expected format <ClassName>.<methodName>, got: " + classAndMethod);
        }
        String className = classAndMethod.substring(0, dotIndex);
        String methodName = classAndMethod.substring(dotIndex + 1);
        if (className.isEmpty() || methodName.isEmpty()) {
            throw new IllegalArgumentException("Invalid class or method name in: " + classAndMethod);
        }
        ReadAction.nonBlocking(() -> {
                    // Locate the PsiClass and PsiMethod in a read action (background thread)
                    PsiClass psiClass = JavaPsiFacade.getInstance(project)
                            .findClass(className, GlobalSearchScope.allScope(project));
                    if (psiClass == null) {
                        throw new RuntimeException("Test class not found: " + className);
                    }
                    PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
                    if (methods.length == 0) {
                        throw new RuntimeException("Test method not found: " + methodName + " in class " + className);
                    }
                    return methods[0];
                }).expireWhen(project::isDisposed)
                .finishOnUiThread(ModalityState.NON_MODAL, psiMethod -> {
                    // Create and configure TestNG run configuration on the UI thread
                    RunManager runManager = RunManager.getInstance(project);
                    String configName = className + "." + methodName;
                    RunnerAndConfigurationSettings settings = runManager.createConfiguration(configName, TestNGConfigurationType.class);
                    TestNGConfiguration config = (TestNGConfiguration) settings.getConfiguration();
                    // Set up configuration to run the specific method
                    ApplicationManager.getApplication().runReadAction(() ->
                            config.beMethodConfiguration(new PsiLocation<>(project, psiMethod))
                    );
                    runManager.addConfiguration(settings);
                    ProgramRunnerUtil.executeConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance());
                }).submit(AppExecutorUtil.getAppExecutorService());
    }
}
