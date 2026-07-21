package org.testin.actions.exports;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ExportHtml extends Export {

    public ExportHtml(final @NotNull SimpleTree tree) {
        super(tree);
    }

    public void exportToFile(final @NotNull Project project, final File destFile,
                             final Map<String, List<TestCaseDto>> sheetsData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)))) {
            writeHtmlDocument(writer, project, sheetsData);
        }
    }

    private void writeHtmlDocument(final @NotNull BufferedWriter writer, final @NotNull Project project,
                                   final Map<String, List<TestCaseDto>> sheetsData) throws IOException {
        writer.write("<!DOCTYPE html>");
        writer.newLine();
        writer.write("<html lang=\"en\">");
        writer.newLine();
        writer.write("<head>");
        writer.newLine();
        writer.write("<meta charset=\"UTF-8\">");
        writer.newLine();
        writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        writer.newLine();
        writer.write("<title>Test Cases Export</title>");
        writer.newLine();
        writer.write("<style>");
        writer.newLine();
        writer.write("  body { font-family: Arial, sans-serif; margin: 20px; }");
        writer.newLine();
        writer.write("  h2 { color: #555; margin-top: 30px; }");
        writer.newLine();
        writer.write("  table { border-collapse: collapse; width: 100%; margin-bottom: 30px; }");
        writer.newLine();
        writer.write("  th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; vertical-align: top; }");
        writer.newLine();
        writer.write("  th { background-color: #f4f4f4; font-weight: bold; }");
        writer.newLine();
        writer.write("  tr:nth-child(even) { background-color: #f9f9f9; }");
        writer.newLine();
        writer.write("  .section-title { margin-top: 20px; }");
        writer.newLine();
        writer.write("</style>");
        writer.newLine();
        writer.write("</head>");
        writer.newLine();
        writer.write("<body>");
        writer.newLine();

        writer.write("<h1>Test Cases Export</h1>");
        writer.newLine();

        int totalExported = 0;

        for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
            final String sheetName = entry.getKey();
            final List<TestCaseDto> testCases = entry.getValue();

            if (testCases.isEmpty()) continue;

            writer.write("<h2>" + htmlEscape(sheetName) + "</h2>");
            writer.newLine();

            writer.write("<table>");
            writer.newLine();

            writer.write("<tr>");
            for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                writer.write("<th>" + htmlEscape(attr.getName()) + "</th>");
            }
            writer.write("</tr>");
            writer.newLine();

            for (TestCaseDto tc : testCases) {
                writer.write("<tr>");
                for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                    String val = attr.getValueExtractor().apply(tc, project);
                    writer.write("<td>" + htmlEscape(val != null ? val : "") + "</td>");
                }
                writer.write("</tr>");
                writer.newLine();
                totalExported++;
            }

            writer.write("</table>");
            writer.newLine();
        }

        writer.write("<p><em>Total test cases exported: " + totalExported + "</em></p>");
        writer.newLine();

        String exportDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz"));
        writer.write("<p><em>Exported on: " + htmlEscape(exportDate) + "</em></p>");
        writer.newLine();

        writer.write("</body>");
        writer.newLine();
        writer.write("</html>");
        writer.newLine();
    }

    private String htmlEscape(final String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&");
                    break;
                case '<':
                    sb.append("<");
                    break;
                case '>':
                    sb.append(">");
                    break;
                case '"':
                    sb.append("&#34;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
