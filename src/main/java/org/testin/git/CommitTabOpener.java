package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.Collection;

public class CommitTabOpener {

    public static void openCommitTabAndListFiles(final @NotNull Project p) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(p);
        Collection<Change> allChanges = changeListManager.getAllChanges();

        Logger.info("--- Files with changes ---");
        for (Change change : allChanges) {
            if (change.getVirtualFile() != null) {
                Logger.info("Changed: " + change.getVirtualFile().getPath());
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager manager = ToolWindowManager.getInstance(p);

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