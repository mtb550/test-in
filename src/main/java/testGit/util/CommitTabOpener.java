package testGit.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import java.util.Collection;

public class CommitTabOpener {

    public static void openCommitTabAndListFiles(Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        System.out.println("--- Files with changes ---");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                System.out.println("Changed: " + change.getVirtualFile().getPath());
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);

            ToolWindow commitWindow = manager.getToolWindow("Commit");

            if (commitWindow != null) {
                commitWindow.activate(null, true);
            } else {
                ToolWindow fallback = manager.getToolWindow("commit");
                if (fallback != null) fallback.activate(null, true);
            }
        });
    }
}