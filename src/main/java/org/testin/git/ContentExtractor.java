package org.testin.git;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.testin.logger.Logger;

public class ContentExtractor {
    public static void printJsonChanges(Change change) {
        ContentRevision revision = (change.getAfterRevision() != null)
                ? change.getAfterRevision()
                : change.getBeforeRevision();

        if (revision == null) return;

        String path = revision.getFile().getPath();

        if (!path.endsWith(".json")) {
            return;
        }

        try {
            String oldContent = (change.getBeforeRevision() != null)
                    ? change.getBeforeRevision().getContent()
                    : "[New File]";

            String newContent = (change.getAfterRevision() != null)
                    ? change.getAfterRevision().getContent()
                    : "[Deleted]";

            Logger.info("File: " + path);
            Logger.info("--- OLD CONTENT ---\n" + oldContent);
            Logger.info("--- NEW CONTENT ---\n" + newContent);
            Logger.info("-------------------");

        } catch (final VcsException ex) {
            Logger.error("Error reading VCS content: " + ex.getMessage());
        }
    }
}
