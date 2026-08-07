package org.testin.git;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import com.intellij.openapi.vcs.changes.ui.ChangeDiffRequestChain;

import java.util.Collections;

public class DiffOpener {
    public static void openSideBySideDiff(Project p, Change change) {
        ChangeDiffRequestProducer producer = ChangeDiffRequestProducer.create(p, change);

        if (producer != null) {
            DiffRequestChain chain = new ChangeDiffRequestChain(Collections.singletonList(producer), 0);
            DiffManager.getInstance().showDiff(p, chain, DiffDialogHints.DEFAULT);
        }
    }
}
