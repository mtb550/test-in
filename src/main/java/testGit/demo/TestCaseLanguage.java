package testGit.demo;

import com.intellij.lang.Language;

public class TestCaseLanguage extends Language {
    public static final TestCaseLanguage INSTANCE = new TestCaseLanguage();

    private TestCaseLanguage() {
        super("TestCase");
        System.out.println("TestCaseLanguage.TestCaseLanguage()");
    }
}