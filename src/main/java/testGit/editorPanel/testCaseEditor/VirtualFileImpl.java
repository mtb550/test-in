package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;

import java.util.List;
import java.util.Objects;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final Directory dir;
    private final List<TestCase> testCases;

    public VirtualFileImpl(@NotNull Directory dir, @NotNull List<TestCase> testCases) {
        super(dir.getName(), FileTypes.UNKNOWN, "");
        this.dir = dir;
        this.testCases = testCases;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirtualFileImpl that)) return false;
        return Objects.equals(dir.getFilePath(), that.dir.getFilePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir.getFilePath());
    }
}