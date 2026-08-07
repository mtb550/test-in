package org.testin.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.util.EditorUtil;


public final class ProjectCloseListenerImpl implements ProjectCloseListener {

    public ProjectCloseListenerImpl() {
    }

    @Override
    public void projectClosed(final @NotNull Project p) {

    }

    @Override
    public void projectClosing(final @NotNull Project p) {

    }

    @Override
    public void projectClosingBeforeSave(final @NotNull Project p) {
        Services.getInstance(p, EditorUtil.class).saveOpen(p);
    }

}
