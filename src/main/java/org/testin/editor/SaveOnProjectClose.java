package org.testin.editor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;


public final class SaveOnProjectClose implements ProjectCloseListener {

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-015
    @Override
    public void projectClosingBeforeSave(final @NotNull Project p) {
        Services.getInstance(p, EditorUtil.class).saveOpen(p);
    }

}
