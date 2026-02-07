package testGit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.LocalChangeList;

public class TestGitMoveHandler {
    public void ensureFilesAreMoved(Project project) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        LocalChangeList automationList = manager.findChangeList("TestGit Automation");

        if (automationList == null) return;

        // Find all changes in the 'Default' list that should be in 'Automation'
        for (Change change : manager.getDefaultChangeList().getChanges()) {
            String path = ChangesUtil.getFilePath(change).getPath();
            if (path.contains("TestGit")) {
                manager.moveChangesTo(automationList, change);
            }
        }
    }
}