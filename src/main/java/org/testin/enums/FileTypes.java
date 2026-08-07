package org.testin.enums;

import com.intellij.openapi.project.Project;
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
public enum FileTypes {
    XLS(
            "XLS",
            ".xls",
            null,
            (p, export, destFile, sheets) -> new ExportExcel(export).exportToFile(p, destFile, sheets),
            (p, imports, importFile) -> new ImportExcel(imports).processImport(p, importFile),
            null
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
            null,
            (p, export, destFile, sheets) -> new ExportJson().exportToFile(p, destFile, sheets),
            (p, imports, importFile) -> new ImportJson().processImport(p, importFile),
            null
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
            null
    ),

    HTML(
            "HTML",
            ".html",
            null,
            (p, export, destFile, sheets) -> new ExportHtml(export).exportToFile(p, destFile, sheets),
            null,
            (p, trDir, tr, detailsMap) -> new TestRunHtmlGenerator().generate(p, trDir, tr, detailsMap).getBytes(StandardCharsets.UTF_8)
    ),

    PDF(
            "PDF",
            ".pdf",
            null,
            null,
            null,
            (p, trDir, tr, detailsMap) -> new TestRunPdfGenerator().generate(p, trDir, tr, detailsMap)
    ),

    WORD(
            "WORD",
            ".docx",
            null,
            null,
            null,
            (p, trDir, tr, detailsMap) -> new TestRunWordGenerator().generate(p, trDir, tr, detailsMap)
    );

    // todo: add XML object.

    private final String label;
    private final String extension;
    private final String infoMessage;
    private final ExportHandler exportHandler;
    private final ImportHandler importHandler;
    private final ReportHandler reportHandler;

    FileTypes(final String label, final String extension, final String infoMessage, final ExportHandler exportHandler, final ImportHandler importHandler, final ReportHandler reportHandler) {
        this.label = label;
        this.extension = extension;
        this.infoMessage = infoMessage;
        this.exportHandler = exportHandler;
        this.importHandler = importHandler;
        this.reportHandler = reportHandler;
    }

    public void exportToFile(final @NotNull Project p, final ExportAction exportAction, final File destFile, final Map<String, List<TestCaseDto>> sheetsData) {
        exportHandler.handle(p, exportAction, destFile, sheetsData);
    }

    public Map<String, List<TestCaseDto>> importToFile(Project p, ImportAction importAction, File importFile) {
        return importHandler.handle(p, importAction, importFile);
    }

    public byte[] generateReport(final @NotNull Project p, final TestRunDirectoryDto trDir, final TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        return reportHandler.handle(p, trDir, tr, detailsMap);
    }

    @FunctionalInterface
    public interface ReportHandler {
        byte[] handle(Project p, TestRunDirectoryDto trDir, TestRunDto tr, Map<UUID, TestCaseDto> detailsMap);
    }

    @FunctionalInterface
    public interface ExportHandler {
        void handle(Project p, ExportAction exportAction, File destFile, Map<String, List<TestCaseDto>> sheetsData);
    }

    @FunctionalInterface
    public interface ImportHandler {
        Map<String, List<TestCaseDto>> handle(Project p, ImportAction importAction, File importFile);
    }
}
