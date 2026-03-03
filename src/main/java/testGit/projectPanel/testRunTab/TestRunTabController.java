package testGit.projectPanel.testRunTab;

import com.intellij.openapi.project.DumbService;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import testGit.pojo.Config;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.projectSelector.ProjectSelector;
import testGit.util.TestRunsDirectoryMapper;

public class TestRunTabController {
    private final ProjectPanel projectPanel;
    @Getter
    private final SimpleTree tree;


    public TestRunTabController(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.tree = new SimpleTree();
    }

    public void setup() {
        if (ProjectSelector.getSelectedProject() == null) {
            tree.getEmptyText().setText("Select a project to view test runs.");
            return;
        }

        tree.setCellRenderer(new TestRunRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.addMouseListener(new MouseAdapterImpl(projectPanel));
        ShortcutHandler.register(tree);

        DumbService.getInstance(Config.getProject()).runWhenSmart(() ->
                TestRunsDirectoryMapper.buildTreeAsync(tree)
        );
    }
}