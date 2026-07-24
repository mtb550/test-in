package org.testin.actions.exports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.importExport.ExportDialog;
import org.testin.pojo.FileTypes;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.InputStream;
import java.util.*;

public class Exports extends DumbAwareAction {

    protected final List<TestEditorAttributes> exportAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isExportable)
            .toList();

    private final @NotNull SimpleTree tree;

    public Exports(final @NotNull SimpleTree tree) {
        super("Export", "Export test cases to a file", AllIcons.ToolbarDecorator.Export);
        this.tree = tree;
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
                    ExportDialog dialog = new ExportDialog(project, exportAttributes, sheets, targetDir);
                    if (!dialog.showAndGet()) return;

                    FileTypes format = dialog.getSelectedFormat();
                    File destFile = dialog.getSelectedFile();
                    if (destFile == null) return;

                    try {
                        format.exportToFile(project, Exports.this, destFile, sheets);
                    } catch (final Exception ex) {
                        Log.error("Export crashed: " + ex.getMessage());
                        ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, Notifier.class).error(project, "Export Failed", ex.getMessage()));
                    }
                });
            }
        });
    }

    public Map<String, List<TestCaseDto>> gatherData(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto dirDto) {
        Map<String, List<TestCaseDto>> allSheets = new LinkedHashMap<>();

        if (dirDto instanceof TestSetDirectoryDto) {
            allSheets.put(targetDirectory.getName(), loadTestCasesInOrder(project, targetDirectory));
        } else {
            VirtualFile[] children = targetDirectory.getChildren();
            if (children != null) {
                for (VirtualFile child : children) {
                    if (child.isDirectory()) {
                        List<TestCaseDto> tcs = loadTestCasesInOrder(project, child);
                        if (!tcs.isEmpty()) {
                            allSheets.put(child.getName(), tcs);
                        }
                    }
                }
            }
        }
        return allSheets;
    }

    public VirtualFile resolveTargetDir(final DirectoryDto dirDto) {
        VirtualFile target = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());
        if (target != null && !target.isDirectory()) {
            target = target.getParent();
        }
        return target;
    }

    public List<TestCaseDto> loadTestCasesInOrder(final @NotNull Project project, final VirtualFile dir) {
        Map<UUID, TestCaseDto> tcMap = new HashMap<>();
        TestCaseDto head = null;

        VirtualFile[] files = dir.getChildren();
        if (files == null) return Collections.emptyList();

        for (VirtualFile file : files) {
            if (!file.isDirectory() && file.getName().endsWith(".json")) {
                try (InputStream is = file.getInputStream()) {
                    TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(is, TestCaseDto.class);
                    if (tc != null) {
                        tcMap.put(tc.getId(), tc);
                        if (Boolean.TRUE.equals(tc.getIsHead())) {
                            head = tc;
                        }
                    }
                } catch (final Exception ex) {
                    Log.error("Loading test cases failed: " + ex.getMessage());
                }
            }
        }

        if (head == null && !tcMap.isEmpty()) {
            return new ArrayList<>(tcMap.values());
        }

        List<TestCaseDto> orderedList = new ArrayList<>();
        TestCaseDto current = head;

        while (current != null) {
            orderedList.add(current);
            if (current.getNext() != null) {
                current = tcMap.get(current.getNext());
            } else {
                current = null;
            }
        }

        return orderedList;
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
