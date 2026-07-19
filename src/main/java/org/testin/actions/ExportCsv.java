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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.*;
import java.util.*;

public class ExportCsv extends DumbAwareAction {

    private final @NotNull SimpleTree tree;

    private final List<TestEditorAttributes> EXPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportValue)
            .toList();

    public ExportCsv(final @NotNull SimpleTree tree) {
        super("Export to CSV", "Export test cases to a CSV file", AllIcons.FileTypes.Csv);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(project, Notifier.class).error(project, "Export Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof DirectoryDto dirDto) ||
                !(dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestSetPackageDirectoryDto || dirDto instanceof TestCasesMainDirectoryDto)) {
            Services.getInstance(project, Notifier.class).error(project, "Export Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Services.getInstance(project, Notifier.class).error(project, "Export Error", "The selected path in the Project Panel is invalid.");
            return;
        }

        FileSaverDescriptor descriptor = new FileSaverDescriptor("Export CSV", "Save test cases as a CSV file", "csv");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);

        String defaultFileName = targetDirectory.getName() + "_Export.csv";
        VirtualFileWrapper wrapper = dialog.save((VirtualFile) null, defaultFileName);

        if (wrapper != null) {
            File destFile = wrapper.getFile();
            processExport(project, destFile, targetDirectory, dirDto);
        }
    }

    private void processExport(final @NotNull Project project, final File destFile, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Exporting test cases to CSV", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Gathering test cases...");

                Map<String, List<TestCaseDto>> sheetsData = gatherData(project, targetDirectory, selectedDirDto);

                if (indicator.isCanceled()) return;

                if (sheetsData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "Export Empty", "No valid test cases found to export in the selected directory."));
                    return;
                }

                indicator.setText("Generating CSV file...");

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)))) {
                    // Write header
                    List<String> headerNames = EXPORT_COLUMNS.stream()
                            .map(TestEditorAttributes::getName)
                            .toList();
                    writer.write(String.join(",", headerNames));
                    writer.newLine();

                    int totalWritten = 0;

                    for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                        if (indicator.isCanceled()) return;

                        List<TestCaseDto> testCases = entry.getValue();

                        for (TestCaseDto tc : testCases) {
                            List<String> rowValues = new ArrayList<>();
                            for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                                String val = attr.getValueExtractor().apply(tc, project);
                                rowValues.add(escapeCsvField(val != null ? val : ""));
                            }
                            writer.write(String.join(",", rowValues));
                            writer.newLine();
                            totalWritten++;
                        }
                    }

                    final int finalTotalWritten = totalWritten;
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).info(project, "Export Complete",
                                    "Successfully exported " + finalTotalWritten + " test cases to:\n" + destFile.getName()));

                } catch (final Exception ex) {
                    Log.error("Export crashed: " + ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).error(project, "Export Failed",
                                    "Failed to save the CSV file:\n" + ex.getMessage()));
                }
            }
        });
    }

    private Map<String, List<TestCaseDto>> gatherData(final @NotNull Project project, VirtualFile targetDirectory, DirectoryDto dirDto) {
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

    private List<TestCaseDto> loadTestCasesInOrder(final @NotNull Project project, final VirtualFile dir) {
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

    /**
     * Escapes a field value for CSV format.
     * If the value contains commas, newlines, or double quotes, wrap it in double quotes
     * and escape any internal double quotes.
     */
    private String escapeCsvField(final String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
