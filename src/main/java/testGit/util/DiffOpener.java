package testGit.util;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import com.intellij.openapi.vcs.changes.ui.ChangeDiffRequestChain;

import java.util.Collections;

public class DiffOpener {
    public static void openSideBySideDiff(Project project, Change change) {
        // 1. Create the Producer (this handles the Before/After logic)
        ChangeDiffRequestProducer producer = ChangeDiffRequestProducer.create(project, change);

        if (producer != null) {
            // 2. Wrap it in a Chain
            // This is the "Provided" type that was causing the error
            DiffRequestChain chain = new ChangeDiffRequestChain(Collections.singletonList(producer), 0);

            // 3. Use the specific method for Chains
            // If the IDE complains about showDiff(project, chain),
            // ensure you aren't trying to assign it to a DiffRequest variable.
            DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
        }
    }
}
