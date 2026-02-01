package testGit.demo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TestCaseFileType extends LanguageFileType {
    public static final TestCaseFileType INSTANCE = new TestCaseFileType();

    private TestCaseFileType() {
        super(TestCaseLanguage.INSTANCE);
        System.out.println("TestCaseFileType.TestCaseFileType()");
    }

    @NotNull
    @Override
    public String getName() {
        //System.out.println("TestCaseFileType.getName()");

        return "TestCaseFile";
    }

    @NotNull
    @Override
    public String getDescription() {
        System.out.println("TestCaseFileType.getDescription()");

        return "Teat Cases table file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        System.out.println("TestCaseFileType.getDefaultExtension()");

        return "tc table";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        //System.out.println("TestCaseFileType.getIcon()");

        return AllIcons.Nodes.DataTables;
    }
}
