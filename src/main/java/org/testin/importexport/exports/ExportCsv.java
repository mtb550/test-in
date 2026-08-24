package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ExportCsv {
    private final @NotNull ExportAction exportAction;

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            final @NotNull List<String> headerNames = exportAction.exportAttributes.stream()
                    .map(TestEditorAttributes::getName)
                    .toList();

            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                for (final TestCaseDto tc : entry.getValue()) {
                    final @NotNull List<String> rowValues = new ArrayList<>();
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

        ExportNotice.show(p, destFile);
    }

    private @NotNull String escapeCsvField(final @NotNull String value) {
        if (value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")
        )
            return "\"" + value.replace("\"", "\"\"") + "\"";

        return value;
    }
}

