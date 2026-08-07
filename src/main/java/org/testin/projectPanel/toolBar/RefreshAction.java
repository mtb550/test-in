package org.testin.projectPanel.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final ProjectPanel pp;
    private final AtomicBoolean refreshGuard = new AtomicBoolean(false);

    public RefreshAction(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        super("Refresh", "Re-index and reload tree", AllIcons.Actions.Refresh);
        this.p = p;
        this.pp = pp;
    }

    public void execute() {
        if (!refreshGuard.compareAndSet(false, true)) {
            Logger.info("Refresh: already in progress, ignoring click");
            return;
        }

        Logger.info("Refresh: re-indexing started");

        final TestProjectDirectoryDto previouslySelected = (TestProjectDirectoryDto) pp.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
        final String previousProjectName = previouslySelected != null ? previouslySelected.getName() : null;

        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        indexer.resetForReindex();

        indexer.indexWithProgress();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            indexer.awaitIndexing();

            Logger.info("Refresh: re-indexing complete, rebuilding tree");

            ApplicationManager.getApplication().invokeLater(() -> {
                pp.getTestProjectSelector().loadTestProjectList();
                pp.setupMainLayout();

                if (previousProjectName != null) {
                    final DefaultComboBoxModel<TestProjectDirectoryDto> list = pp.getTestProjectSelector().getTestProjectList();
                    for (int i = 0; i < list.getSize(); i++) {
                        TestProjectDirectoryDto tp = list.getElementAt(i);
                        if (tp.getName().equals(previousProjectName)) {
                            pp.getTestProjectSelector().getSelectedTestProject().setSelectedItem(tp);
                            pp.getTestProjectSelector().filterByTestProject(tp);
                            break;
                        }
                    }
                }

                refreshGuard.set(false);
                Logger.info("Refresh: tree rebuilt");
            });
        });
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {

        boolean hasTree = pp.getProjectTree().getMainTree() != null;
        e.getPresentation().setEnabled(hasTree);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}