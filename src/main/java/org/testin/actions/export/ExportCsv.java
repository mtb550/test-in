package org.testin.actions.export;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
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
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportCsv extends ExportBase {

    public ExportCsv(final @NotNull SimpleTree tree) {
        super(tree, "Export to CSV", "Export test cases to a CSV file", AllIcons.FileTypes.Csv);
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

    public void exportToFile(final @NotNull Project project, final File destFile,
                             final Map<String, List<TestCaseDto>> sheetsData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)))) {
            List<String> headerNames = EXPORT_COLUMNS.stream()
                    .map(TestEditorAttributes::getName)
                    .toList();
            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                List<TestCaseDto> testCases = entry.getValue();
                for (TestCaseDto tc : testCases) {
                    List<String> rowValues = new ArrayList<>();
                    for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                        String val = attr.getValueExtractor().apply(tc, project);
                        rowValues.add(escapeCsvField(val != null ? val : ""));
                    }
                    writer.write(String.join(",", rowValues));
                    writer.newLine();
                }
            }
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
                    NotificationAction openAction = NotificationAction.createSimple("Open file", () -> {
                        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
                        if (vf != null) {
                            Services.getInstance(project, Tools.class).openWithAssociatedProgram(project, vf);
                        }
                    });
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).infoWithActions(project,
                                    "Export Complete",
                                    "Successfully exported " + finalTotalWritten + " test cases to:\n" + destFile.getName(),
                                    openAction));

                } catch (final Exception ex) {
                    Log.error("Export crashed: " + ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).error(project, "Export Failed",
                                    "Failed to save the CSV file:\n" + ex.getMessage()));
                }
            }
        });
    }

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
