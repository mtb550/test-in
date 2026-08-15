package org.testin.importexport.exports;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ExportCsv {
    private final @NotNull ExportAction exportAction;

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile,
                             final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            final List<String> headerNames = exportAction.exportAttributes.stream()
                    .map(TestEditorAttributes::getName)
                    .toList();

            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                for (final TestCaseDto tc : entry.getValue()) {
                    final List<String> rowValues = new ArrayList<>();
                    for (final TestEditorAttributes attr : exportAction.exportAttributes) {
                        rowValues.add(escapeCsvField(attr.getTestValueExtractor().execute(tc, p)));
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
                Services.getInstance(p, Notifier.class).infoWithActions(p, "Export Complete", "Exported to: " + destFile.getName(),
                        NotificationAction.createSimple("Open file", () -> {
                            final VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
                            Services.getInstance(p, Tools.class).openWithAssociatedProgram(p, vf);
                        }))
        );
    }

    private @NotNull String escapeCsvField(final @Nullable String value) {
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

