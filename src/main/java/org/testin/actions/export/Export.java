package org.testin.actions.export;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.FileTypes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.ExportImportPreviewDialog;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.List;
import java.util.Map;

public class Export extends ExportBase {

    public Export(final @NotNull SimpleTree tree) {
        super(tree, "Export", "Export test cases to a file", AllIcons.ToolbarDecorator.Export);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();
        if (!(userObject instanceof DirectoryDto dirDto)) return;

        VirtualFile targetDir = resolveTargetDir(dirDto);
        if (targetDir == null) return;

        processExport(project, targetDir, dirDto);
    }

    private void processExport(final @NotNull Project project, final VirtualFile targetDir, final DirectoryDto dirDto) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Exporting test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                Map<String, List<TestCaseDto>> sheets = gatherData(project, targetDir, dirDto);
                if (sheets.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "Export Empty", "No test cases found."));
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    ExportImportPreviewDialog dialog = new ExportImportPreviewDialog(project, sheets, targetDir);
                    if (!dialog.showAndGet()) return;

                    String format = dialog.getSelectedFormat();
                    File destFile = dialog.getSelectedFile();
                    if (destFile == null) return;

                    try {
                        switch (FileTypes.fromLabel(format)) {
                            case CSV -> new ExportCsv(tree).exportToFile(project, destFile, sheets);
                            case EXCEL -> new ExportExcel(tree).exportToFile(project, destFile, sheets);
                            case HTML -> new ExportHtml(tree).exportToFile(project, destFile, sheets);
                            case JSON -> new ExportJson(tree).exportToFile(project, destFile, sheets);
                        }

                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(project, Notifier.class).infoWithActions(project,
                                        "Export Complete", "Exported to: " + destFile.getName(),
                                        ExportBase.createOpenAction(project, destFile, format)));

                    } catch (final Exception ex) {
                        Log.error("Export crashed: " + ex.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(project, Notifier.class).error(project, "Export Failed", ex.getMessage()));
                    }
                });
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        e.getPresentation().setEnabled(path != null &&
                tree.getSelectionCount() == 1 &&
                ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof DirectoryDto
        );
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
