package testGit.editorPanel.testRunEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.EditorType;
import testGit.pojo.TestCase;
import testGit.pojo.TestRun;

import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Paths;
import java.util.List;

@Getter
@Setter
public class VirtualFileImpl extends LightVirtualFile {
    private final String runPath;
    private final DefaultTreeModel testCasesTreeModel;
    private final List<TestCase> testCases; // Added this
    private TestRun metadata; // New field to store dialog data
    private EditorType editorType;

    public VirtualFileImpl(@NotNull String runPath, @NotNull DefaultTreeModel treeModel, List<TestCase> testCases, EditorType editorType) {
        super(String.format("Run: %s", Paths.get(runPath).getFileName()));
        this.runPath = runPath;
        this.testCasesTreeModel = treeModel;
        this.testCases = testCases; // Store them
        this.editorType = editorType;
    }

}