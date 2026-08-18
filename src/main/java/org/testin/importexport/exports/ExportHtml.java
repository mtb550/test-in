package org.testin.importexport.exports;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ExportHtml {
    private static final @NotNull Map<Character, String> HTML_ESCAPES = Map.of(
            '&', "&amp;",
            '<', "&lt;",
            '>', "&gt;",
            '"', "&#34;",
            '\'', "&#39;"
    );
    private final @NotNull ExportAction exportAction;

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile,
                             final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        // Explicit UTF-8: the document declares <meta charset="UTF-8">, and the platform
        // default charset (e.g. cp1252 on Windows) would mangle non-ASCII text.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            writeHtmlDocument(writer, p, sheetsData);
        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(p, Notifier.class)
                        .infoWithActions(p, "Exported", destFile.getName(), NotificationAction.createSimple("Open file", () -> BrowserUtil.browse(destFile.toURI().toString()))));
    }

    private void writeHtmlDocument(final @NotNull BufferedWriter writer, final @NotNull Project p,
                                   final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try {
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

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                final String sheetName = entry.getKey();
                final List<TestCaseDto> testCases = entry.getValue();

                if (testCases.isEmpty()) continue;

                writer.write("<h2>" + htmlEscape(sheetName) + "</h2>");
                writer.newLine();

                writer.write("<table>");
                writer.newLine();

                writer.write("<tr>");
                for (final TestEditorAttributes attr : exportAction.exportAttributes) {
                    writer.write("<th>" + htmlEscape(attr.getName()) + "</th>");
                }
                writer.write("</tr>");
                writer.newLine();

                for (final TestCaseDto tc : testCases) {
                    writer.write("<tr>");
                    for (final TestEditorAttributes attr : exportAction.exportAttributes) {
                        writer.write("<td>" + htmlEscape(attr.getTestValueExtractor().execute(tc, p)) + "</td>");
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

            final String exportDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz"));
            writer.write("<p><em>Exported on: " + htmlEscape(exportDate) + "</em></p>");
            writer.newLine();

            writer.write("</body>");
            writer.newLine();
            writer.write("</html>");
            writer.newLine();
        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private @NotNull String htmlEscape(final @Nullable String value) {
        if (value == null) return "";

        final StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            final String rep = HTML_ESCAPES.get(c);
            if (rep != null) sb.append(rep);
            else sb.append(c);
        }
        return sb.toString();
    }
}

