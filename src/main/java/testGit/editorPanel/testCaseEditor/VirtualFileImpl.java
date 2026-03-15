package testGit.editorPanel.testCaseEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import testGit.editorPanel.FileType;
import testGit.pojo.TestCase;
import testGit.pojo.TestPackage;

import java.util.List;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final TestPackage pkg;
    private final List<TestCase> testCases;

    public VirtualFileImpl(TestPackage pkg, List<TestCase> testCases) {
        super(pkg.getName());
        this.pkg = pkg;
        this.testCases = testCases;
        this.setFileType(FileType.TEST_CASE);
    }

    @Override
    public boolean isValid() {
        return true;
    }


}