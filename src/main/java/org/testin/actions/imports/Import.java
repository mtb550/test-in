package org.testin.actions.imports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.FileTypes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.ImportPreviewDialog;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Import extends ImportBase {

    public Import(final @NotNull SimpleTree tree) {
        super(tree, "Import", "Import test cases from a file", AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ImportContext ctx = validateTreeSelection(e);
        if (ctx == null) return;

        final Project project = ctx.project();
        final DirectoryDto finalDirDto = ctx.dirDto();
        final VirtualFile finalTargetDir = ctx.targetDirectory();
        final DefaultMutableTreeNode parentNode = ctx.parentNode();

        ImportPreviewDialog dialog = new ImportPreviewDialog(project, new LinkedHashMap<>());
        dialog.setImportFileLoader((file, format) -> parseImportFile(project, file, format));

        if (dialog.showAndGet()) {
            Map<String, List<TestCaseDto>> selectedCasesBySheet = dialog.getSelectedTestCasesBySheet();

            if (selectedCasesBySheet.isEmpty()) {
                Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                return;
            }

            executeImportWriteAction(project, finalTargetDir, finalDirDto, parentNode, dialog, selectedCasesBySheet, "Import");
        } else {
            Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled from preview dialog.");
        }
    }

    private Map<String, List<TestCaseDto>> parseImportFile(final @NotNull Project project, final File importFile, final FileTypes format) {
        return switch (format) {
            case CSV -> parseCsvFile(project, importFile);
            case XLS, XLSX -> parseExcelFile(project, importFile);
            case JSON -> parseJsonFile(project, importFile);
            case HTML -> {
                Services.getInstance(project, Notifier.class).warn(project, "Unsupported", "HTML import is not supported.");
                yield new LinkedHashMap<>();
            }
        };
    }

    private Map<String, List<TestCaseDto>> parseCsvFile(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            List<TestCaseDto> testCases = new ImportCsv(tree).parseFile(project, file);
            if (!testCases.isEmpty()) {
                String name = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
                result.put(name, testCases);
            }
        } catch (final Exception ex) {
            Log.error("CSV import parse failed: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "CSV Parse Error", ex.getMessage());
        }
        return result;
    }

    private Map<String, List<TestCaseDto>> parseExcelFile(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            Map<String, List<TestCaseDto>> parsed = new ImportExcel(tree).parseFile(project, file);
            result.putAll(parsed);
        } catch (final Exception ex) {
            Log.error("Excel import parse failed: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "Excel Parse Error", ex.getMessage());
        }
        return result;
    }

    private Map<String, List<TestCaseDto>> parseJsonFile(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            Map<String, List<TestCaseDto>> parsed = new ImportJson(tree).parseFile(project, file);
            result.putAll(parsed);
        } catch (final Exception ex) {
            Log.error("JSON import parse failed: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "JSON Parse Error", ex.getMessage());
        }
        return result;
    }
}
