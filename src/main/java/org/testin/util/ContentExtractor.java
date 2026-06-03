package org.testin.util;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.testin.util.logger.Log;

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

            Log.info("File: " + path);
            Log.info("--- OLD CONTENT ---\n" + oldContent);
            Log.info("--- NEW CONTENT ---\n" + newContent);
            Log.info("-------------------");

        } catch (VcsException e) {
            Log.error("Error reading VCS content: " + e.getMessage());
        }
    }
}
