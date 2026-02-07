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
        // 1. Get the list of files that have changes
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        System.out.println("Files with changes:");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                System.out.println(change.getVirtualFile().getPath());
            }
        }

        // 2. Open the Commit Tool Window
        // Use "invokeAndWait" or "invokeLater" because UI changes must happen on the EDT thread
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow commitWindow = ToolWindowManager.getInstance(project).getToolWindow("commit");

            if (commitWindow != null) {
                commitWindow.activate(() -> {
                    System.out.println("Commit tab is now visible.");
                }, true);
            } else {
                // Fallback for older versions of IntelliJ where it was part of "Version Control"
                ToolWindow vcsWindow = ToolWindowManager.getInstance(project).getToolWindow("Version Control");
                if (vcsWindow != null) {
                    vcsWindow.activate(null);
                }
            }
        });
    }
}