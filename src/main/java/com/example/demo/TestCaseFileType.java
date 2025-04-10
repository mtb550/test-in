package com.example.demo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TestCaseFileType extends LanguageFileType {
    public static final TestCaseFileType INSTANCE = new TestCaseFileType();

    private TestCaseFileType() {
        super(TestCaseLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "TestCaseFile";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Teat Cases table file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "tc table";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.Nodes.DataTables;
    }
}
