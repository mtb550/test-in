package com.example.demo;

import com.intellij.lang.Language;

public class TestCaseLanguage extends Language {
    public static final TestCaseLanguage INSTANCE = new TestCaseLanguage();

    private TestCaseLanguage() {
        super("TestCase");
    }
}