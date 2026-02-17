package testGit.editorPanel.testRunEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Paths;
import java.util.List;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final String runPath;
    private final DefaultTreeModel testCasesTreeModel;
    private final List<TestCase> testCases; // Added this

    public VirtualFileImpl(@NotNull String runPath, @NotNull DefaultTreeModel treeModel, List<TestCase> testCases) {
        super(String.format("Run: %s", Paths.get(runPath).getFileName()));
        this.runPath = runPath;
        this.testCasesTreeModel = treeModel;
        this.testCases = testCases; // Store them
    }

}