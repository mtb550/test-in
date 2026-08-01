package org.testin.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
import org.testin.util.EditorUtil;
import org.testin.util.services.Services;


public final class ProjectCloseListenerImpl implements ProjectCloseListener {

    public ProjectCloseListenerImpl() {
    }

    @Override
    public void projectClosed(final @NotNull Project project) {

    }

    @Override
    public void projectClosing(final @NotNull Project project) {

    }

    @Override
    public void projectClosingBeforeSave(final @NotNull Project project) {
        Services.getInstance(project, EditorUtil.class).saveOpen(project);
    }

}
