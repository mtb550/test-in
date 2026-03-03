package testGit.editorPanel.testCaseEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;

import java.util.List;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final Directory dir;
    private final List<TestCase> testCases;

    public VirtualFileImpl(@NotNull Directory dir, @NotNull List<TestCase> testCases) {
        super(dir.getName());
        this.dir = dir;
        this.testCases = testCases;
    }

    @Override
    public boolean isValid() {
        return true;
    }


}