package org.testin.pojo;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.testin.actions.exports.*;
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
            (project, export, destFile, sheets) -> new ExportExcel(export).exportToFile(project, destFile, sheets)
    ),

    XLSX(
            "XLSX",
            ".xlsx",
            """
                    To ensure a successful import, your Excel file should contain the following column headers (case-insensitive):
                    
                    %s
                    
                    Note: Missing columns will safely default to empty values.
                    You can also download a ready-to-use sample file using the button below.""",
            (project, export, destFile, sheets) -> new ExportExcel(export).exportToFile(project, destFile, sheets)
    ),

    JSON(
            "JSON",
            ".json",
            null,
            (project, export, destFile, sheets) -> new ExportJson(export).exportToFile(project, destFile, sheets)
    ),

    CSV(
            "CSV",
            ".csv",
            """
                    To ensure a successful import, your CSV file should contain the following column headers (case-insensitive):
                    
                    %s
                    
                    Note: Missing columns will safely default to empty values.
                    The CSV should use comma as delimiter. Values containing commas or newlines must be quoted with double quotes.""",
            (project, export, destFile, sheets) -> new ExportCsv(export).exportToFile(project, destFile, sheets)
    ),

    HTML(
            "HTML",
            ".html",
            null,
            (project, export, destFile, sheets) -> new ExportHtml(export).exportToFile(project, destFile, sheets)
    );

    private final String label;
    private final String extension;
    private final String infoMessage;
    private final ExportHandler handler;

    FileTypes(final String label, final String extension, final String infoMessage, final ExportHandler handler) {
        this.label = label;
        this.extension = extension;
        this.infoMessage = infoMessage;
        this.handler = handler;
    }

    public void exportToFile(final Project project, final Exports exports, final File destFile, final Map<String, List<TestCaseDto>> sheetsData) {
        handler.handle(project, exports, destFile, sheetsData);
    }

    @FunctionalInterface
    public interface ExportHandler {
        void handle(Project project, Exports exports, File destFile, Map<String, List<TestCaseDto>> sheetsData);
    }
}
