package testGit.util;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;

public class ContentExtractor {
    public static void printJsonChanges(Change change) {
        ContentRevision revision = (change.getAfterRevision() != null)
                ? change.getAfterRevision()
                : change.getBeforeRevision();

        if (revision == null) return;

        String path = revision.getFile().getPath();

        if (!path.toLowerCase().endsWith(".json")) {
            return;
        }

        try {
            String oldContent = (change.getBeforeRevision() != null)
                    ? change.getBeforeRevision().getContent()
                    : "[New File]";

            String newContent = (change.getAfterRevision() != null)
                    ? change.getAfterRevision().getContent()
                    : "[Deleted]";

            System.out.println("File: " + path);
            System.out.println("--- OLD CONTENT ---\n" + oldContent);
            System.out.println("--- NEW CONTENT ---\n" + newContent);
            System.out.println("-------------------");

        } catch (VcsException e) {
            System.err.println("Error reading VCS content: " + e.getMessage());
        }
    }
}
