package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Refresh extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final AtomicBoolean refreshGuard = new AtomicBoolean(false);

    public Refresh(final ProjectPanel projectPanel) {
        super("Refresh", "Re-index and reload tree", AllIcons.Actions.Refresh);
        this.projectPanel = projectPanel;
    }

    public void execute() {
        if (!refreshGuard.compareAndSet(false, true)) {
            Log.info("Refresh: already in progress, ignoring click");
            return;
        }

        final Project project = projectPanel.getProject();

        Log.info("Refresh: re-indexing started");

        final TestProjectDirectoryDto previouslySelected = projectPanel.getTestProjectSelector() != null
                ? (TestProjectDirectoryDto) projectPanel.getTestProjectSelector().getSelectedTestProject().getSelectedItem()
                : null;
        final String previousProjectName = previouslySelected != null ? previouslySelected.getName() : null;

        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
        indexer.resetForReindex();

        indexer.indexWithProgress();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            indexer.awaitIndexing();

            Log.info("Refresh: re-indexing complete, rebuilding tree");

            ApplicationManager.getApplication().invokeLater(() -> {
                projectPanel.setupMainLayout();

                if (previousProjectName != null) {
                    final DefaultComboBoxModel<TestProjectDirectoryDto> list = projectPanel.getTestProjectSelector().getTestProjectList();
                    for (int i = 0; i < list.getSize(); i++) {
                        TestProjectDirectoryDto tp = list.getElementAt(i);
                        if (tp.getName().equals(previousProjectName)) {
                            projectPanel.getTestProjectSelector().getSelectedTestProject().setSelectedItem(tp);
                            projectPanel.getTestProjectSelector().filterByTestProject(tp);
                            break;
                        }
                    }
                }

                refreshGuard.set(false);
                Log.info("Refresh: tree rebuilt");
            });
        });
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        execute();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        boolean hasTree = projectPanel.getProjectTree() != null && projectPanel.getProjectTree().getMainTree() != null;
        e.getPresentation().setEnabled(hasTree);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}