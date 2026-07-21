package org.testin.actions.exports;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportCsv extends Export {

    public ExportCsv(final @NotNull SimpleTree tree) {
        super(tree);
    }

    public void exportToFile(final @NotNull Project project, final File destFile,
                             final Map<String, List<TestCaseDto>> sheetsData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)))) {
            List<String> headerNames = EXPORT_COLUMNS.stream()
                    .map(TestEditorAttributes::getName)
                    .toList();
            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                List<TestCaseDto> testCases = entry.getValue();
                for (TestCaseDto tc : testCases) {
                    List<String> rowValues = new ArrayList<>();
                    for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                        String val = attr.getValueExtractor().apply(tc, project);
                        rowValues.add(escapeCsvField(val != null ? val : ""));
                    }
                    writer.write(String.join(",", rowValues));
                    writer.newLine();
                }
            }
        }
    }

    private String escapeCsvField(final String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
