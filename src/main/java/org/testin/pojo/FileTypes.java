package org.testin.pojo;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.testin.actions.exports.*;
import org.testin.actions.imports.ImportCsv;
import org.testin.actions.imports.ImportExcel;
import org.testin.actions.imports.ImportJson;
import org.testin.actions.imports.Imports;
import org.testin.pojo.dto.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
public enum FileTypes {
    XLS(
            "XLS",
            ".xls",
            null,
            (project, export, destFile, sheets) -> new ExportExcel(export).exportToFile(project, destFile, sheets),
            (project, imports, importFile) -> new ImportExcel(imports).processImport(project, importFile)
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
            (project, imports, importFile) -> new ImportExcel(imports).processImport(project, importFile)
    ),

    JSON(
            "JSON",
            ".json",
            null,
            (project, export, destFile, sheets) -> new ExportJson(export).exportToFile(project, destFile, sheets),
            (project, imports, importFile) -> new ImportJson().processImport(project, importFile)
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
            (project, imports, importFile) -> new ImportCsv(imports).processImport(project, importFile)
    ),

    HTML(
            "HTML",
            ".html",
            null,
            (project, export, destFile, sheets) -> new ExportHtml(export).exportToFile(project, destFile, sheets),
            null
    );

    private final String label;
    private final String extension;
    private final String infoMessage;
    private final ExportHandler exportHandler;
    private final ImportHandler importHandler;

    FileTypes(final String label, final String extension, final String infoMessage, final ExportHandler exportHandler, final ImportHandler importHandler) {
        this.label = label;
        this.extension = extension;
        this.infoMessage = infoMessage;
        this.exportHandler = exportHandler;
        this.importHandler = importHandler;
    }

    public void exportToFile(final Project project, final Exports exports, final File destFile, final Map<String, List<TestCaseDto>> sheetsData) {
        exportHandler.handle(project, exports, destFile, sheetsData);
    }

    public Map<String, List<TestCaseDto>> importToFile(Project project, Imports imports, File importFile) {
        return importHandler.handle(project, imports, importFile);
    }

    @FunctionalInterface
    public interface ExportHandler {
        void handle(Project project, Exports exports, File destFile, Map<String, List<TestCaseDto>> sheetsData);
    }

    @FunctionalInterface
    public interface ImportHandler {
        Map<String, List<TestCaseDto>> handle(Project project, Imports imports, File importFile);
    }
}
