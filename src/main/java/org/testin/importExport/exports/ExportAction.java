package org.testin.importExport.exports;

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
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;
import org.testin.util.Mapper;

import javax.swing.tree.TreePath;
import java.io.File;
import java.io.InputStream;
import java.util.*;

public class ExportAction extends DumbAwareAction {

    protected final @NotNull List<TestEditorAttributes> exportAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isExportable)
            .toList();
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;

    public ExportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Export", "Export test cases to a file", AllIcons.ToolbarDecorator.Export);
        this.p = p;
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());
        if (!(userObject instanceof DirectoryDto dirDto)) return;

        final VirtualFile targetDir = resolveTargetDir(dirDto);
        if (targetDir == null) return;

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Exporting test cases", true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                final Map<String, List<TestCaseDto>> sheets = gatherData(targetDir, dirDto);
                if (sheets.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).warn(p, "Export Empty", "No test cases found."));
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    final ExportDialog dialog = new ExportDialog(p, exportAttributes, sheets, targetDir);
                    if (!dialog.showAndGet()) return;

                    final FileTypes format = dialog.getSelectedFormat();
                    final File destFile = dialog.getSelectedFile();
                    if (destFile == null) return;

                    try {
                        format.exportToFile(p, ExportAction.this, destFile, sheets);
                    } catch (final Exception ex) {
                        Logger.error("Export crashed: " + ex.getMessage());
                        ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class).error(p, "Export Failed", ex.getMessage()));
                    }
                });
            }
        });
    }

    public @NotNull Map<String, List<TestCaseDto>> gatherData(final @NotNull VirtualFile targetDirectory,
                                                              final @NotNull DirectoryDto dirDto) {
        final Map<String, List<TestCaseDto>> allSheets = new LinkedHashMap<>();

        if (dirDto instanceof TestSetDirectoryDto) {
            allSheets.put(targetDirectory.getName(), loadTestCasesInOrder(p, targetDirectory));
        } else {
            final VirtualFile[] children = targetDirectory.getChildren();
            if (children != null) {
                for (final VirtualFile child : children) {
                    if (child.isDirectory()) {
                        final List<TestCaseDto> tcs = loadTestCasesInOrder(p, child);
                        if (!tcs.isEmpty()) {
                            allSheets.put(child.getName(), tcs);
                        }
                    }
                }
            }
        }
        return allSheets;
    }

    /** Null when the path is not in the VFS; a file resolves to its parent directory. */
    public @Nullable VirtualFile resolveTargetDir(final @NotNull DirectoryDto dirDto) {
        final VirtualFile target = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());
        if (target == null) return null;

        return target.isDirectory() ? target : target.getParent();
    }

    public @NotNull List<TestCaseDto> loadTestCasesInOrder(final @NotNull Project p, final @NotNull VirtualFile dir) {
        final Map<UUID, TestCaseDto> tcMap = new HashMap<>();
        TestCaseDto head = null;

        final VirtualFile[] files = dir.getChildren();
        if (files == null) return Collections.emptyList();

        for (final VirtualFile file : files) {
            if (!file.isDirectory() && file.getName().endsWith(".json")) {
                try (InputStream is = file.getInputStream()) {
                    final TestCaseDto tc = Services.getInstance(p, Mapper.class).readValue(is, TestCaseDto.class);
                    tcMap.put(tc.getId(), tc);
                    if (Boolean.TRUE.equals(tc.getIsHead())) {
                        head = tc;
                    }
                } catch (final Exception ex) {
                    Logger.error("Loading test cases failed: " + ex.getMessage());
                }
            }
        }

        if (head == null && !tcMap.isEmpty()) {
            return new ArrayList<>(tcMap.values());
        }

        final List<TestCaseDto> orderedList = new ArrayList<>();
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
        final int selectionCount = tree.getSelectionCount();

        if (selectionCount != 1 || path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());

        e.getPresentation().setEnabled(userObject instanceof DirectoryDto dir && dir.isTestCaseContainer());

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
