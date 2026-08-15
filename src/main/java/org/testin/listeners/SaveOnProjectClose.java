package org.testin.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.util.EditorUtil;


public final class SaveOnProjectClose implements ProjectCloseListener {

    @Override
    public void projectClosingBeforeSave(final @NotNull Project p) {
        Services.getInstance(p, EditorUtil.class).saveOpen(p);
    }

}
