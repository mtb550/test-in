package com.example.Runner;

/*
 * إذا كان هدفك هو جعل تنفيذ الاختبار أسرع وكنت تعرف بالفعل اسم الفئة (مثل com.example.MyTest)، فهذا هو النهج الأفضل.
 */

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.project.Project;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;

public class TestNGRunnerByClassName {

    public static void runTestClass(Project project, String fullyQualifiedClassName) {
        // إنشاء تكوين تشغيل TestNG جديد
        TestNGConfigurationType configType = TestNGConfigurationType.getInstance();
        RunManager runManager = RunManager.getInstance(project);

        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
                "Run " + fullyQualifiedClassName, configType.getConfigurationFactories()[0]);
        TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();

        // تعيين التكوين يدوياً باستخدام اسم الفئة
        configuration.getPersistantData().TEST_OBJECT = TestType.CLASS.getType();
        configuration.getPersistantData().MAIN_CLASS_NAME = fullyQualifiedClassName;

        // مسح أي بيانات موجودة
        configuration.getPersistantData().getPatterns().clear();

        // حفظ وتشغيل
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }
}
