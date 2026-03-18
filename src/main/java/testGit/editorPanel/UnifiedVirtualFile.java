package testGit.editorPanel;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import lombok.Setter;
import testGit.pojo.EditorType;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.pojo.dto.dirs.TestRunDirectoryDto;
import testGit.pojo.dto.dirs.TestSetDirectoryDto;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

@Getter
@Setter
public class UnifiedVirtualFile extends LightVirtualFile {

    // --- خصائص مشتركة (Shared Properties) ---
    private final DirectoryDto directoryDto; // يمكن أن يكون TestSet أو TestRun
    private final List<TestCaseDto> testCaseDtos;

    // --- خصائص خاصة بـ Test Run (ستكون null في حالة Test Case) ---
    private ProjectPanel projectPanel;
    private DefaultTreeModel testCasesTreeModel;
    private TestRunDto metadata;
    private EditorType editorType;

    // 🌟 1. المُنشئ الخاص بـ Test Case
    public UnifiedVirtualFile(TestSetDirectoryDto directory, List<TestCaseDto> testCaseDtos) {
        super(directory.getName());
        this.directoryDto = directory;
        this.testCaseDtos = testCaseDtos;
        this.setFileType(FileType.TEST_CASE);
    }

    // 🌟 2. المُنشئ الخاص بـ Test Run
    public UnifiedVirtualFile(TestRunDirectoryDto directory, DefaultTreeModel treeModel, List<TestCaseDto> testCaseDtos, EditorType editorType, ProjectPanel projectPanel) {
        super(directory.getName());
        this.directoryDto = directory;
        this.testCaseDtos = testCaseDtos;
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

    public TestSetDirectoryDto getTestSet() {
        return directoryDto instanceof TestSetDirectoryDto ? (TestSetDirectoryDto) directoryDto : null;
    }

    public TestRunDirectoryDto getTestRunPkg() {
        return directoryDto instanceof TestRunDirectoryDto ? (TestRunDirectoryDto) directoryDto : null;
    }
}