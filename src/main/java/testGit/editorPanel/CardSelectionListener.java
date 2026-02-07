package testGit.editorPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.ui.components.JBList;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.util.ContentExtractor;
import testGit.viewPanel.ViewPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.List;

/**
 * Listens for selection changes in the TestCase list and updates the details view.
 */
public class CardSelectionListener implements ListSelectionListener {
    private final JBList<TestCase> list;

    public CardSelectionListener(final JBList<TestCase> list) {
        this.list = list;
    }

    public static void hideChangesSilently(Project project) {
        ChangeListManager manager = ChangeListManager.getInstance(project);

        // 1. Find your TestGit changes
        List<Change> toHide = manager.getAllChanges().stream()
                .filter(c -> {
                    FilePath path = ChangesUtil.getFilePath(c);
                    return path.getPath().contains("/TestGit/");
                })
                .toList();

        if (toHide.isEmpty()) return;

        // 2. Create a specific list
        LocalChangeList testGitList = manager.findChangeList("TestGit Automation");
        if (testGitList == null) {
            testGitList = manager.addChangeList("TestGit Automation", "Internal data - do not commit manually");
        }

        // 3. Move them. They disappear from the "Default" list.
        manager.moveChangesTo(testGitList, toHide.toArray(new Change[0]));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // Ensure we only trigger once per selection (not during the drag process)
        if (!e.getValueIsAdjusting()) {
            TestCase selected = list.getSelectedValue();
            if (selected != null) {
                // Auto-update details window on selection change
                ViewPanel.show(selected);
                //VersionControlTabOpener.openCommitTabAndListFiles(Config.getProject());
                //CommitTabOpener.openCommitTabAndListFiles(Config.getProject());


//                ChangeListManager changeListManager = ChangeListManager.getInstance(Config.getProject());
//                List<Change> changes = new ArrayList<>(changeListManager.getAllChanges());
//                DiffOpener.openSideBySideDiff(Config.getProject(),changes.get(0));
//                DiffOpener.openSideBySideDiff(Config.getProject(),changes.get(1));
//                DiffOpener.openSideBySideDiff(Config.getProject(),changes.get(2));

                ChangeListManager.getInstance(Config.getProject()).getAllChanges().forEach(ContentExtractor::printJsonChanges);


                //listener - good choice
//                ChangeListManager.getInstance(Config.getProject()).addChangeListListener(new ChangeListListener() {
//                    @Override
//                    public void changeListUpdateDone() {
//                        // Auto-move TestGit files whenever the IDE finishes a VCS scan
//                        hideChangesSilently(Config.getProject());
//                    }
//                });


            }
        }
    }
}