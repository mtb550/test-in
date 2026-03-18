package testGit.editorPanel;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import lombok.Setter;
import testGit.pojo.EditorType;
import testGit.pojo.mappers.TestCase;
import testGit.pojo.mappers.TestRun;
import testGit.pojo.tree.dirs.Directory;
import testGit.pojo.tree.dirs.TestRunDirectory;
import testGit.pojo.tree.dirs.TestSetDirectory;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

@Getter
@Setter
public class UnifiedVirtualFile extends LightVirtualFile {

    // --- خصائص مشتركة (Shared Properties) ---
    private final Directory directory; // يمكن أن يكون TestSet أو TestRun
    private final List<TestCase> testCases;

    // --- خصائص خاصة بـ Test Run (ستكون null في حالة Test Case) ---
    private ProjectPanel projectPanel;
    private DefaultTreeModel testCasesTreeModel;
    private TestRun metadata;
    private EditorType editorType;

    // 🌟 1. المُنشئ الخاص بـ Test Case
    public UnifiedVirtualFile(TestSetDirectory directory, List<TestCase> testCases) {
        super(directory.getName());
        this.directory = directory;
        this.testCases = testCases;
        this.setFileType(FileType.TEST_CASE);
    }

    // 🌟 2. المُنشئ الخاص بـ Test Run
    public UnifiedVirtualFile(TestRunDirectory directory, DefaultTreeModel treeModel, List<TestCase> testCases, EditorType editorType, ProjectPanel projectPanel) {
        super(directory.getName());
        this.directory = directory;
        this.testCases = testCases;
        this.testCasesTreeModel = treeModel;
        this.editorType = editorType;
        this.projectPanel = projectPanel;
        this.setFileType(FileType.TEST_RUN);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    // --- دوال مساعدة لجلب الكائن بالنوع الصحيح بسهولة ---

    public TestSetDirectory getTestSet() {
        return directory instanceof TestSetDirectory ? (TestSetDirectory) directory : null;
    }

    public TestRunDirectory getTestRunPkg() {
        return directory instanceof TestRunDirectory ? (TestRunDirectory) directory : null;
    }
}