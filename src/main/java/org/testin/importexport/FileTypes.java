package org.testin.importexport;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.exports.ExportCsv;
import org.testin.importexport.exports.ExportExcel;
import org.testin.importexport.exports.ExportHtml;
import org.testin.importexport.exports.ExportJson;
import org.testin.importexport.imports.ImportCsv;
import org.testin.importexport.imports.ImportExcel;
import org.testin.importexport.imports.ImportJson;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.report.generators.TestRunExcelGenerator;
import org.testin.report.generators.TestRunHtmlGenerator;
import org.testin.report.generators.TestRunPdfGenerator;
import org.testin.report.generators.TestRunWordGenerator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum FileTypes {
    /**
     * Import only. The legacy workbook still has to be recognised by its
     * extension so a tester can import one, but nothing here writes it: XLS was
     * handed the xlsx exporter, so choosing it produced xlsx bytes in a file
     * named .xls. Excel warned about the mismatch on every open and some tools
     * refused the file outright, and because nothing in the plugin failed the
     * complaint only ever came back from whoever received it.
     */
    XLS(
            "XLS",
            ".xls",
            "",
            ExportHandler.UNSUPPORTED,
            (p, importFile) -> new ImportExcel().processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    XLSX(
            "Excel",
            ".xlsx",
            """
                    To ensure a successful import, your Excel file should contain the following column headers (case-insensitive):
                    
                    %s
                    
                    Note: Missing columns will safely default to empty values.""",
            (p, destFile, sheets) -> new ExportExcel().exportToFile(p, destFile, sheets),
            (p, importFile) -> new ImportExcel().processImport(p, importFile),
            (p, trDir, tr, detailsMap) -> new TestRunExcelGenerator().generate(p, trDir, tr, detailsMap)
    ),

    JSON(
            "JSON",
            ".json",
            "",
            (p, destFile, sheets) -> new ExportJson().exportToFile(p, destFile, sheets),
            (p, importFile) -> new ImportJson().processImport(p, importFile),
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
            (p, destFile, sheets) -> new ExportCsv().exportToFile(p, destFile, sheets),
            (p, importFile) -> new ImportCsv().processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    HTML(
            "HTML",
            ".html",
            "",
            (p, destFile, sheets) -> new ExportHtml().exportToFile(p, destFile, sheets),
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
            "Word",
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

    /**
     * The format that can read this file name, empty when nothing can. Only
     * formats with an import handler count; matching an .html file would NPE
     * downstream.
     * <p>
     * Here rather than on the dialog's document listener, which is where it was
     * until #291: which extension belongs to which format is what this enum is,
     * and a listener that debounces keystrokes had no business answering it.
     */
    public static @NotNull Optional<FileTypes> importerFor(final @NotNull String fileName) {
        return Arrays.stream(values())
                .filter(type -> type.isImportable() && fileName.endsWith(type.getExtension()))
                .findFirst();
    }

    /**
     * The extensions an import understands, as a tester would say them.
     */
    public static @NotNull String importableExtensions() {
        return Arrays.stream(values())
                .filter(FileTypes::isImportable)
                .map(FileTypes::getExtension)
                .collect(Collectors.joining(", "));
    }

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        // Checked here rather than left to the handler, so the failure names the format.
        if (!isExportable()) throw new IllegalStateException(label + " cannot be exported to");
        exportHandler.execute(p, destFile, sheetsData);
    }

    public @NotNull Map<String, List<TestCaseDto>> importToFile(final @NotNull Project p, final @NotNull File importFile) {
        if (!isImportable()) throw new IllegalStateException(label + " cannot be imported from");
        return importHandler.execute(p, importFile);
    }

    public byte @NotNull [] generateReport(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        if (!isReportable()) throw new IllegalStateException(label + " has no report generator");
        return reportHandler.execute(p, trDir, tr, detailsMap);
    }
}
