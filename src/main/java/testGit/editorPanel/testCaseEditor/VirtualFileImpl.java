package testGit.editorPanel.testCaseEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import testGit.editorPanel.FileType;
import testGit.pojo.TestSet;
import testGit.pojo.mappers.TestCaseJsonMapper;

import java.util.List;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final TestSet pkg;
    private final List<TestCaseJsonMapper> testCaseJsonMappers;

    public VirtualFileImpl(TestSet pkg, List<TestCaseJsonMapper> testCaseJsonMappers) {
        super(pkg.getName());
        this.pkg = pkg;
        this.testCaseJsonMappers = testCaseJsonMappers;
        this.setFileType(FileType.TEST_CASE);
    }

    @Override
    public boolean isValid() {
        return true;
    }


}