package org.testin.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

public class ExportExcel extends DumbAwareAction {

    private final SimpleTree tree;

    private final List<TestEditorAttributes> EXPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportValue)
            .toList();

    public ExportExcel(final SimpleTree tree) {
        super("Export to Excel", "Export test cases to an Excel file", AllIcons.FileTypes.MicrosoftWindows);
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

        FileSaverDescriptor descriptor = new FileSaverDescriptor("Export Excel", "Save test cases as an Excel file", "xlsx");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, Config.getProject());

        String defaultFileName = targetDirectory.getName() + "_Export.xlsx";
        VirtualFileWrapper wrapper = dialog.save((VirtualFile) null, defaultFileName);

        if (wrapper != null) {
            File destFile = wrapper.getFile();
            processExportWithPoi(destFile, targetDirectory, dirDto);
        }
    }

    private void processExportWithPoi(final File destFile, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Exporting test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Gathering test cases...");

                Map<String, List<TestCaseDto>> sheetsData = gatherData(targetDirectory, selectedDirDto);

                if (indicator.isCanceled()) return;

                if (sheetsData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().warn("Export Empty", "No valid test cases found to export in the selected directory."));
                    return;
                }

                indicator.setText("Generating Excel file...");

                try (Workbook workbook = new XSSFWorkbook()) {
                    CellStyle headerStyle = workbook.createCellStyle();
                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerStyle.setFont(headerFont);

                    for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                        if (indicator.isCanceled()) return;

                        String safeSheetName = entry.getKey().replaceAll("[\\\\/*?\\[\\]]", "_");
                        if (safeSheetName.length() > 31) {
                            safeSheetName = safeSheetName.substring(0, 31);
                        }

                        while (workbook.getSheet(safeSheetName) != null) {
                            safeSheetName = safeSheetName.substring(0, 28) + "...";
                        }

                        Sheet sheet = workbook.createSheet(safeSheetName);
                        List<TestCaseDto> testCases = entry.getValue();

                        Row headerRow = sheet.createRow(0);
                        for (int i = 0; i < EXPORT_COLUMNS.size(); i++) {
                            Cell cell = headerRow.createCell(i);
                            cell.setCellValue(EXPORT_COLUMNS.get(i).getName());
                            cell.setCellStyle(headerStyle);
                        }

                        int rowIndex = 1;
                        for (TestCaseDto tc : testCases) {
                            Row row = sheet.createRow(rowIndex++);
                            for (int i = 0; i < EXPORT_COLUMNS.size(); i++) {
                                Cell cell = row.createCell(i);
                                String val = EXPORT_COLUMNS.get(i).getValueExtractor().apply(tc);
                                cell.setCellValue(val != null ? val : "");
                            }
                        }

                        for (int i = 0; i < EXPORT_COLUMNS.size(); i++) {
                            sheet.autoSizeColumn(i);
                        }
                    }

                    indicator.setText("Saving file to disk...");
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        workbook.write(fos);
                    }

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Export Complete", "Successfully exported test cases to:\n" + destFile.getName()));

                } catch (Exception ex) {
                    System.err.println("Export crashed: " + ex.getMessage());
                    ex.printStackTrace(System.err);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Export Failed", "Failed to save the Excel file:\n" + ex.getMessage()));
                }
            }
        });
    }

    private Map<String, List<TestCaseDto>> gatherData(VirtualFile targetDirectory, DirectoryDto dirDto) {
        Map<String, List<TestCaseDto>> allSheets = new LinkedHashMap<>();
        ObjectMapper mapper = Config.getMapper();

        if (dirDto instanceof TestSetDirectoryDto) {

            allSheets.put(targetDirectory.getName(), loadTestCasesInOrder(targetDirectory, mapper));
        } else {

            VirtualFile[] children = targetDirectory.getChildren();
            if (children != null) {
                for (VirtualFile child : children) {
                    if (child.isDirectory()) {
                        List<TestCaseDto> tcs = loadTestCasesInOrder(child, mapper);
                        if (!tcs.isEmpty()) {
                            allSheets.put(child.getName(), tcs);
                        }
                    }
                }
            }
        }
        return allSheets;
    }

    private List<TestCaseDto> loadTestCasesInOrder(final VirtualFile dir, final ObjectMapper mapper) {
        Map<UUID, TestCaseDto> tcMap = new HashMap<>();
        TestCaseDto head = null;

        VirtualFile[] files = dir.getChildren();
        if (files == null) return Collections.emptyList();

        for (VirtualFile file : files) {
            if (!file.isDirectory() && file.getName().endsWith(".json")) {
                try (InputStream is = file.getInputStream()) {
                    TestCaseDto tc = mapper.readValue(is, TestCaseDto.class);
                    tcMap.put(tc.getId(), tc);
                    if (Boolean.TRUE.equals(tc.getIsHead())) {
                        head = tc;
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