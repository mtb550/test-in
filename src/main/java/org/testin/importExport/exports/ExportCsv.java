package org.testin.importExport.exports;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportCsv {
    private final @NotNull ExportAction exportAction;

    public ExportCsv(final @NotNull ExportAction exportAction) {
        this.exportAction = exportAction;
    }

    public void exportToFile(final @NotNull Project project, final File destFile, final Map<String, List<TestCaseDto>> sheetsData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)))) {
            List<String> headerNames = exportAction.exportAttributes.stream()
                    .map(TestEditorAttributes::getName)
                    .toList();

            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                List<TestCaseDto> testCases = entry.getValue();
                for (TestCaseDto tc : testCases) {
                    List<String> rowValues = new ArrayList<>();
                    for (TestEditorAttributes attr : exportAction.exportAttributes) {
                        String val = attr.getValueExtractor().apply(tc, project);
                        rowValues.add(escapeCsvField(val != null ? val : ""));
                    }
                    writer.write(String.join(",", rowValues));
                    writer.newLine();
                }
            }
        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(project, Notifier.class).infoWithActions(project, "Export Complete", "Exported to: " + destFile.getName(),
                        NotificationAction.createSimple("Open file", () -> {
                            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
                            Services.getInstance(project, Tools.class).openWithAssociatedProgram(project, vf);
                        }))
        );
    }

    private String escapeCsvField(final String value) {
        if (value == null) return "";

        if (value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")
        )
            return "\"" + value.replace("\"", "\"\"") + "\"";

        return value;
    }
}
