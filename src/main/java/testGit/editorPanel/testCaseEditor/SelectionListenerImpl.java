package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.util.ContentExtractor;
import testGit.viewPanel.ViewPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.List;

@AllArgsConstructor
public class SelectionListenerImpl implements ListSelectionListener {
    private final JBList<TestCase> list;

    public static void hideChangesSilently(Project project) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        List<Change> toHide = manager.getAllChanges().stream()
                .filter(c -> {
                    FilePath path = ChangesUtil.getFilePath(c);
                    return path.getPath().contains("/TestGit/");
                })
                .toList();

        if (toHide.isEmpty()) return;

        LocalChangeList testGitList = manager.findChangeList("TestGit Internal");
        if (testGitList == null) {
            testGitList = manager.addChangeList("TestGit Internal", "Auto-managed by plugin");
        }
        manager.moveChangesTo(testGitList, toHide.toArray(new Change[0]));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            TestCase selected = list.getSelectedValue();
            if (selected != null) {
                // Check if the "Details" ToolWindow is already open/active
                ToolWindow toolWindow = ToolWindowManager.getInstance(Config.getProject())
                        .getToolWindow("Details");

                if (toolWindow != null && toolWindow.isVisible()) {
                    // Update the details without forcing the window to 'pop' or grab focus
                    ViewPanel.show(selected);
                }

                // Keep your existing change tracking logic
                com.intellij.openapi.vcs.changes.ChangeListManager.getInstance(Config.getProject())
                        .getAllChanges()
                        .forEach(ContentExtractor::printJsonChanges);
            }
        }
    }
}