package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunMarker;
import org.testin.pojo.TestRunStatus;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.ui.TestRunStatusMenu;
import org.testin.util.FilesUtil;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

// todo, to be refactored
public class SetTestRunStatus extends DumbAwareAction {
    final SimpleTree tree;

    public SetTestRunStatus(final @NotNull SimpleTree tree) {
        super("Set Status", "Set test run status", AllIcons.Nodes.Test);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (userObject instanceof TestRunDirectoryDto testRunDto) {
            new TestRunStatusMenu(project, selectedStatus -> {
                TestRunMarker marker = testRunDto.getMarker();
                marker.setStatus(selectedStatus);
                marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                Log.trace("Status changed -> " + testRunDto.getName() + " = " + selectedStatus.getLabel());

                persistMarker(project, testRunDto, selectedStatus);

                tree.repaint();
            }).show();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();
        boolean enabled = userObject instanceof TestRunDirectoryDto dir &&
                dir.getMarker() != null &&
                dir.getMarker().getStatus() != TestRunStatus.COMPLETED &&
                dir.getMarker().getStatus() != TestRunStatus.CLOSED;

        e.getPresentation().setEnabled(enabled);
    }

    private void persistMarker(final Project project, final TestRunDirectoryDto tr, final TestRunStatus newStatus) {
        Path markerPath = tr.getPath().resolve(".tr");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                TestRunMarker marker = Services.getInstance(project, Mapper.class).readValue(markerPath.toFile(), TestRunMarker.class);
                if (marker != null) {
                    marker.setStatus(newStatus);
                    Services.getInstance(project, FilesUtil.class).write(project, markerPath, marker);
                }
            } catch (Exception ex) {
                Log.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
