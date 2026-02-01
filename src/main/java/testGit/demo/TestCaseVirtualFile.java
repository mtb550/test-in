package testGit.demo;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import testGit.pojo.TestCase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Getter
public class TestCaseVirtualFile extends LightVirtualFile {
    private final String featurePath; // المسار الكامل للمجلد المعني
    private final List<TestCase> testCases;

    public TestCaseVirtualFile(String featurePath, List<TestCase> testCases) {
        // نستخدم اسم المجلد فقط كعنوان للتبويب ليظهر بشكل جميل للمستخدم
        super(extractFolderName(featurePath), TestCaseFileType.INSTANCE, "");
        this.featurePath = featurePath;
        this.testCases = testCases;
        System.out.println("TestCaseVirtualFile.TestCaseVirtualFile()");
    }

    /**
     * دالة مساعدة لاستخراج اسم المجلد من المسار الكامل
     * مثال: /home/user/Test/Login -> يعيد Login
     */
    private static String extractFolderName(String path) {
        System.out.println("TestCaseVirtualFile.extractFolderName()");
        try {
            Path p = Paths.get(path);
            return p.getFileName().toString();
        } catch (Exception e) {
            return "Test Cases";
        }
    }

    @Override
    public boolean equals(Object o) {
        // infinite sout
        //System.out.println("TestCaseVirtualFile.equals()");
        if (this == o) return true;
        if (!(o instanceof TestCaseVirtualFile that)) return false;
        // المقارنة الآن تعتمد على المسار لضمان عدم تكرار فتح نفس المجلد
        return Objects.equals(featurePath, that.featurePath);
    }

    @Override
    public int hashCode() {
        // infinite sout
        //System.out.println("TestCaseVirtualFile.hashCode()");
        return Objects.hash(featurePath);
    }
}