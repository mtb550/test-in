package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.testin.logger.Logger;

import java.util.Collection;

public class VersionControlTabOpener {

    public static void openCommitTabAndListFiles(Project p) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(p);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        Logger.info("Files with changes:");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                Logger.info(change.getVirtualFile().getPath());
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow commitWindow = ToolWindowManager.getInstance(p).getToolWindow("commit");

            if (commitWindow != null) {
                commitWindow.activate(() -> {
                    Logger.info("Commit tab is now visible.");
                }, true);
            } else {
                ToolWindow vcsWindow = ToolWindowManager.getInstance(p).getToolWindow("Version Control");
                if (vcsWindow != null) {
                    vcsWindow.activate(null);
                }
            }
        });
    }
}