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
        // 1. Get the list of files that have changes
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        System.out.println("--- Files with changes ---");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                System.out.println("Changed: " + change.getVirtualFile().getPath());
            }
        }

        // 2. Open the "Commit" Tab using the direct ID string
        // This avoids the 'internal' access error entirely.
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);

            // "Commit" is the hardcoded ID for the tab in your screenshot
            ToolWindow commitWindow = manager.getToolWindow("Commit");

            if (commitWindow != null) {
                commitWindow.activate(null, true);
            } else {
                // fallback for some versions where it is lowercase
                ToolWindow fallback = manager.getToolWindow("commit");
                if (fallback != null) fallback.activate(null, true);
            }
        });
    }
}