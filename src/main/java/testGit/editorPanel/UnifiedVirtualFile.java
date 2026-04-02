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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UnifiedVirtualFile extends LightVirtualFile {

    // Shared Properties
    private final DirectoryDto directoryDto;
    private final List<TestCaseDto> testCaseDtos;

    // Test Run
    private ProjectPanel projectPanel;
    private DefaultTreeModel testCasesTreeModel;
    private TestRunDto metadata;
    private EditorType editorType;

    // Auto Complete Steps
    private Set<String> uniqueStepsCache = null;

    // Test Case
    public UnifiedVirtualFile(TestSetDirectoryDto directory, List<TestCaseDto> testCaseDtos) {
        super(directory.getName());
        this.directoryDto = directory;
        this.testCaseDtos = testCaseDtos;
        this.setFileType(FileType.TEST_CASE);
    }

    // Test Run
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

    public TestSetDirectoryDto getTestSet() {
        return directoryDto instanceof TestSetDirectoryDto ? (TestSetDirectoryDto) directoryDto : null;
    }

    public TestRunDirectoryDto getTestRunPkg() {
        return directoryDto instanceof TestRunDirectoryDto ? (TestRunDirectoryDto) directoryDto : null;
    }

    public Set<String> getUniqueSteps() {
        if (uniqueStepsCache == null) {
            uniqueStepsCache = testCaseDtos.stream()
                    .filter(tc -> tc.getSteps() != null)
                    .flatMap(tc -> tc.getSteps().stream())
                    .filter(step -> step != null && !step.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        return uniqueStepsCache;
    }

    public void addNewStepToCache(String newStep) {
        getUniqueSteps();

        if (newStep != null && !newStep.trim().isEmpty()) {
            uniqueStepsCache.add(newStep.trim());
        }
    }
}