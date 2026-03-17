package testGit.editorPanel.testRunEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import lombok.Setter;
import testGit.editorPanel.FileType;
import testGit.pojo.EditorType;
import testGit.pojo.TestRun;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.pojo.mappers.TestRunJsonMapper;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

@Getter
@Setter
public class VirtualFileImpl extends LightVirtualFile {
    private final ProjectPanel projectPanel;
    private final TestRun pkg;
    private final DefaultTreeModel testCasesTreeModel;
    private final List<TestCaseJsonMapper> testCaseJsonMappers;
    private TestRunJsonMapper metadata;
    private EditorType editorType;

    public VirtualFileImpl(TestRun pkg, DefaultTreeModel treeModel, List<TestCaseJsonMapper> testCaseJsonMappers, EditorType editorType, ProjectPanel projectPanel) {
        super(pkg.getName());
        this.pkg = pkg;
        this.testCaseJsonMappers = testCaseJsonMappers;
        this.testCasesTreeModel = treeModel;
        this.editorType = editorType;
        this.projectPanel = projectPanel;
        this.setFileType(FileType.TEST_RUN);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}