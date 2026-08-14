package org.testin.enums;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.generateReport.generators.TestRunExcelGenerator;
import org.testin.generateReport.generators.TestRunHtmlGenerator;
import org.testin.generateReport.generators.TestRunPdfGenerator;
import org.testin.generateReport.generators.TestRunWordGenerator;
import org.testin.importExport.exports.*;
import org.testin.importExport.imports.ImportAction;
import org.testin.importExport.imports.ImportCsv;
import org.testin.importExport.imports.ImportExcel;
import org.testin.importExport.imports.ImportJson;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public enum FileTypes {
    XLS(
            "XLS",
            ".xls",
            "",
            (p, export, destFile, sheets) -> new ExportExcel(export).exportToFile(p, destFile, sheets),
            (p, imports, importFile) -> new ImportExcel(imports).processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    XLSX(
            "XLSX",
            ".xlsx",
            """
                    To ensure a successful import, your Excel file should contain the following column headers (case-insensitive):
                    
                    %s
                    
                    Note: Missing columns will safely default to empty values.
                    You can also download a ready-to-use sample file using the button below.""",
            (p, export, destFile, sheets) -> new ExportExcel(export).exportToFile(p, destFile, sheets),
            (p, imports, importFile) -> new ImportExcel(imports).processImport(p, importFile),
            (p, trDir, tr, detailsMap) -> new TestRunExcelGenerator().generate(p, trDir, tr, detailsMap)
    ),

    JSON(
            "JSON",
            ".json",
            "",
            (p, export, destFile, sheets) -> new ExportJson().exportToFile(p, destFile, sheets),
            (p, imports, importFile) -> new ImportJson().processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    CSV(
            "CSV",
            ".csv",
            """
                    To ensure a successful import, your CSV file should contain the following column headers (case-insensitive):
                    
                    %s
                    
                    Note: Missing columns will safely default to empty values.
                    The CSV should use comma as delimiter. Values containing commas or newlines must be quoted with double quotes.""",
            (p, export, destFile, sheets) -> new ExportCsv(export).exportToFile(p, destFile, sheets),
            (p, imports, importFile) -> new ImportCsv(imports).processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    HTML(
            "HTML",
            ".html",
            "",
            (p, export, destFile, sheets) -> new ExportHtml(export).exportToFile(p, destFile, sheets),
            ImportHandler.UNSUPPORTED,
            (p, trDir, tr, detailsMap) -> new TestRunHtmlGenerator().generate(p, trDir, tr, detailsMap).getBytes(StandardCharsets.UTF_8)
    ),

    PDF(
            "PDF",
            ".pdf",
            "",
            ExportHandler.UNSUPPORTED,
            ImportHandler.UNSUPPORTED,
            (p, trDir, tr, detailsMap) -> new TestRunPdfGenerator().generate(p, trDir, tr, detailsMap)
    ),

    WORD(
            "WORD",
            ".docx",
            "",
            ExportHandler.UNSUPPORTED,
            ImportHandler.UNSUPPORTED,
            (p, trDir, tr, detailsMap) -> new TestRunWordGenerator().generate(p, trDir, tr, detailsMap)
    );

    // todo: add XML object.

    private final @NotNull String label;
    private final @NotNull String extension;

    /**
     * The import-dialog hint; empty for formats that need no explanation.
     */
    private final @NotNull String infoMessage;

    // PDF and WORD are report-only and HTML has no importer, so those carry the
    // handler's UNSUPPORTED instance. Ask what a format supports with the
    // is* methods below - a handler is always present, so its absence is not
    // the question to ask.
    private final @NotNull ExportHandler exportHandler;
    private final @NotNull ImportHandler importHandler;
    private final @NotNull ReportHandler reportHandler;

    public boolean isExportable() {
        return exportHandler != ExportHandler.UNSUPPORTED;
    }

    public boolean isImportable() {
        return importHandler != ImportHandler.UNSUPPORTED;
    }

    public boolean isReportable() {
        return reportHandler != ReportHandler.UNSUPPORTED;
    }

    public void exportToFile(final @NotNull Project p, final @NotNull ExportAction exportAction,
                             final @NotNull File destFile,
                             final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        // Checked here rather than left to the handler, so the failure names the format.
        if (!isExportable()) throw new IllegalStateException(label + " cannot be exported to");
        exportHandler.execute(p, exportAction, destFile, sheetsData);
    }

    public @NotNull Map<String, List<TestCaseDto>> importToFile(final @NotNull Project p,
                                                                final @NotNull ImportAction importAction,
                                                                final @NotNull File importFile) {
        if (!isImportable()) throw new IllegalStateException(label + " cannot be imported from");
        return importHandler.execute(p, importAction, importFile);
    }

    public byte @NotNull [] generateReport(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                                           final @NotNull TestRunDto tr,
                                           final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        if (!isReportable()) throw new IllegalStateException(label + " has no report generator");
        return reportHandler.execute(p, trDir, tr, detailsMap);
    }
}
