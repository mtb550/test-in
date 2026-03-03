package testGit.editorPanel.testRunEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.EditorType;
import testGit.pojo.TestCase;
import testGit.pojo.TestRun;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Paths;
import java.util.List;

@Getter
@Setter
public class VirtualFileImpl extends LightVirtualFile {
    private final ProjectPanel projectPanel;
    private final String runPath;
    private final DefaultTreeModel testCasesTreeModel;
    private final List<TestCase> testCases;
    private TestRun metadata;
    private EditorType editorType;

    public VirtualFileImpl(@NotNull String runPath, @NotNull DefaultTreeModel treeModel, List<TestCase> testCases, EditorType editorType, ProjectPanel projectPanel) {
        super(String.format("Run: %s", Paths.get(runPath).getFileName()));
        this.projectPanel = projectPanel;
        this.runPath = runPath;
        this.testCasesTreeModel = treeModel;
        this.testCases = testCases;
        this.editorType = editorType;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}