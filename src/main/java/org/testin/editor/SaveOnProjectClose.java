package org.testin.editor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;


public final class SaveOnProjectClose implements ProjectCloseListener {

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-015
    @Override
    public void projectClosingBeforeSave(final @NotNull Project p) {
        final @NotNull EditorUtil editors = Services.getInstance(p, EditorUtil.class);

        // Written down first, then closed - in that order, because closing is
        // what makes there be nothing left to write down. Both halves belong
        // here rather than one of them inside the other: this listener is the
        // only thing that knows the IDE is about to save its tab list.
        editors.saveOpen(p);
        editors.closeAll(p);
    }

}
