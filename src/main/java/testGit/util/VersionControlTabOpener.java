package testGit.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import java.util.Collection;

public class VersionControlTabOpener {

    public static void openCommitTabAndListFiles(Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        System.out.println("Files with changes:");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                System.out.println(change.getVirtualFile().getPath());
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow commitWindow = ToolWindowManager.getInstance(project).getToolWindow("commit");

            if (commitWindow != null) {
                commitWindow.activate(() -> {
                    System.out.println("Commit tab is now visible.");
                }, true);
            } else {
                ToolWindow vcsWindow = ToolWindowManager.getInstance(project).getToolWindow("Version Control");
                if (vcsWindow != null) {
                    vcsWindow.activate(null);
                }
            }
        });
    }
}