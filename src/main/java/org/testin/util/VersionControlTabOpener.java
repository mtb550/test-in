package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.testin.util.logger.Log;

import java.util.Collection;

public class VersionControlTabOpener {

    public static void openCommitTabAndListFiles(Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        Log.info("Files with changes:");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                Log.info(change.getVirtualFile().getPath());
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow commitWindow = ToolWindowManager.getInstance(project).getToolWindow("commit");

            if (commitWindow != null) {
                commitWindow.activate(() -> {
                    Log.info("Commit tab is now visible.");
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