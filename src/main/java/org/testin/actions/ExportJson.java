package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.Mapper;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class ExportJson extends DumbAwareAction {

    private final SimpleTree tree;

    public ExportJson(final SimpleTree tree) {
        super("Export to JSON", "Export test cases to a JSON file", AllIcons.FileTypes.Json);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Notifier.getInstance().error("Export Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof DirectoryDto dirDto) ||
                !(dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestSetPackageDirectoryDto || dirDto instanceof TestCasesMainDirectoryDto)) {
            Notifier.getInstance().error("Export Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Notifier.getInstance().error("Export Error", "The selected path in the Project Panel is invalid.");
            return;
        }

        FileSaverDescriptor descriptor = new FileSaverDescriptor("Export JSON", "Save test cases as a JSON file", "json");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, Config.getProject());

        String defaultFileName = targetDirectory.getName() + "_Export.json";
        VirtualFileWrapper wrapper = dialog.save((VirtualFile) null, defaultFileName);

        if (wrapper != null) {
            File destFile = wrapper.getFile();
            processExportWithJson(destFile, targetDirectory, dirDto);
        }
    }

    private void processExportWithJson(final File destFile, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Exporting test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Gathering test cases...");

                Map<String, List<TestCaseDto>> directoryData = gatherData(targetDirectory, selectedDirDto);

                if (indicator.isCanceled()) return;

                if (directoryData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().warn("Export Empty", "No valid test cases found to export in the selected directory."));
                    return;
                }

                indicator.setText("Generating JSON file...");

                try {
                    byte[] jsonBytes = Mapper.writeValueAsBytes(directoryData);
                    Files.write(destFile.toPath(), jsonBytes);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Export Complete", "Successfully exported test cases to:\n" + destFile.getName()));

                } catch (Exception ex) {
                    System.err.println("Export crashed: " + ex.getMessage());
                    ex.printStackTrace(System.err);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Export Failed", "Failed to save the JSON file:\n" + ex.getMessage()));
                }
            }
        });
    }

    private Map<String, List<TestCaseDto>> gatherData(VirtualFile targetDirectory, DirectoryDto dirDto) {
        Map<String, List<TestCaseDto>> allSets = new LinkedHashMap<>();

        if (dirDto instanceof TestSetDirectoryDto) {
            allSets.put(targetDirectory.getName(), loadTestCasesInOrder(targetDirectory));
        } else {
            VirtualFile[] children = targetDirectory.getChildren();
            if (children != null) {
                for (VirtualFile child : children) {
                    if (child.isDirectory()) {
                        List<TestCaseDto> tcs = loadTestCasesInOrder(child);
                        if (!tcs.isEmpty()) {
                            allSets.put(child.getName(), tcs);
                        }
                    }
                }
            }
        }
        return allSets;
    }

    private List<TestCaseDto> loadTestCasesInOrder(final VirtualFile dir) {
        Map<UUID, TestCaseDto> tcMap = new HashMap<>();
        TestCaseDto head = null;

        VirtualFile[] files = dir.getChildren();
        if (files == null) return Collections.emptyList();

        for (VirtualFile file : files) {
            if (!file.isDirectory() && file.getName().endsWith(".json")) {
                try (InputStream is = file.getInputStream()) {

                    // Use the new centralized Mapper (InputStream overload)
                    TestCaseDto tc = Mapper.readValue(is, TestCaseDto.class);

                    if (tc != null) {
                        tcMap.put(tc.getId(), tc);
                        if (Boolean.TRUE.equals(tc.getIsHead())) {
                            head = tc;
                        }
                    }
                } catch (Exception ignored) {
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
        final int selectionCount = tree.getSelectionCount();

        if (selectionCount != 1 || path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = selectedNode.getUserObject();

        e.getPresentation().setEnabled(userObject instanceof TestSetDirectoryDto ||
                userObject instanceof TestSetPackageDirectoryDto ||
                userObject instanceof TestCasesMainDirectoryDto);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}