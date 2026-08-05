package org.testin.enums;

import com.intellij.openapi.project.Project;
import lombok.Getter;
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
            (project, export, destFile, sheets) -> new ExportExcel(export).exportToFile(project, destFile, sheets),
            (project, imports, importFile) -> new ImportExcel(imports).processImport(project, importFile),
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
            (project, export, destFile, sheets) -> new ExportExcel(export).exportToFile(project, destFile, sheets),
            (project, imports, importFile) -> new ImportExcel(imports).processImport(project, importFile),
            (project, trDir, tr, detailsMap) -> new TestRunExcelGenerator().generate(project, trDir, tr, detailsMap)
    ),

    JSON(
            "JSON",
            ".json",
            null,
            (project, export, destFile, sheets) -> new ExportJson().exportToFile(project, destFile, sheets),
            (project, imports, importFile) -> new ImportJson().processImport(project, importFile),
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
            (project, export, destFile, sheets) -> new ExportCsv(export).exportToFile(project, destFile, sheets),
            (project, imports, importFile) -> new ImportCsv(imports).processImport(project, importFile),
            null
    ),

    HTML(
            "HTML",
            ".html",
            null,
            (project, export, destFile, sheets) -> new ExportHtml(export).exportToFile(project, destFile, sheets),
            null,
            (project, trDir, tr, detailsMap) -> new TestRunHtmlGenerator().generate(project, trDir, tr, detailsMap).getBytes(StandardCharsets.UTF_8)
    ),

    PDF(
            "PDF",
            ".pdf",
            null,
            null,
            null,
            (project, trDir, tr, detailsMap) -> new TestRunPdfGenerator().generate(project, trDir, tr, detailsMap)
    ),

    WORD(
            "WORD",
            ".docx",
            null,
            null,
            null,
            (project, trDir, tr, detailsMap) -> new TestRunWordGenerator().generate(project, trDir, tr, detailsMap)
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

    public void exportToFile(final Project project, final ExportAction exportAction, final File destFile, final Map<String, List<TestCaseDto>> sheetsData) {
        exportHandler.handle(project, exportAction, destFile, sheetsData);
    }

    public Map<String, List<TestCaseDto>> importToFile(Project project, ImportAction importAction, File importFile) {
        return importHandler.handle(project, importAction, importFile);
    }

    public byte[] generateReport(final Project project, final TestRunDirectoryDto trDir, final TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        return reportHandler.handle(project, trDir, tr, detailsMap);
    }

    @FunctionalInterface
    public interface ReportHandler {
        byte[] handle(Project project, TestRunDirectoryDto trDir, TestRunDto tr, Map<UUID, TestCaseDto> detailsMap);
    }

    @FunctionalInterface
    public interface ExportHandler {
        void handle(Project project, ExportAction exportAction, File destFile, Map<String, List<TestCaseDto>> sheetsData);
    }

    @FunctionalInterface
    public interface ImportHandler {
        Map<String, List<TestCaseDto>> handle(Project project, ImportAction importAction, File importFile);
    }
}
