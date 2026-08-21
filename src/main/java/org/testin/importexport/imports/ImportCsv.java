package org.testin.importexport.imports;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@AllArgsConstructor
public class ImportCsv {
    private final @NotNull ImportAction importAction;

    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            final @NotNull List<TestCaseDto> testCases = parseFile(p, file);
            if (!testCases.isEmpty()) {
                final @NotNull String name = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
                result.put(name, testCases);
            }
        } catch (final Exception ex) {
            Logger.error("CSV import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "CSV Parse Error", ex.getMessage());
        }
        return result;
    }

    public @NotNull List<TestCaseDto> parseFile(final @NotNull Project p, final @NotNull File file) {
        return parseCsvFile(file, p);
    }

    private @NotNull List<TestCaseDto> parseCsvFile(final @NotNull File file, final @NotNull Project p) {
        final @NotNull List<TestCaseDto> result = new ArrayList<>();
        final @NotNull List<String[]> records = parseCsvRecords(file);

        if (records.isEmpty()) return result;

        final String @NotNull[] headers = records.getFirst();
        final @NotNull Map<String, Integer> headerIndexMap = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            final @NotNull String headerName = headers[i].trim();
            for (final TestEditorAttributes reqCol : importAction.importAttributes) {
                if (reqCol.getName().equalsIgnoreCase(headerName)) {
                    headerIndexMap.put(reqCol.getName().toLowerCase(), i);
                }
            }
        }

        for (int r = 1; r < records.size(); r++) {
            final String @NotNull[] values = records.get(r);

            // Every field comes from endRecord below, which builds them from a
            // StringBuilder - so a field is blank or it is text, never absent.
            if (Arrays.stream(values).allMatch(String::isBlank)) continue;

            final @NotNull TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());

            for (final TestEditorAttributes attr : TestEditorAttributes.values()) {
                if (attr.can(Can.IMPORT)) {
                    // A column this file does not carry, or a short row that
                    // stops before it, both read as blank.
                    final @NotNull String rawValue = Optional.ofNullable(headerIndexMap.get(attr.getName().toLowerCase()))
                            .filter(colIndex -> colIndex < values.length)
                            .map(colIndex -> values[colIndex].trim())
                            .orElse("");
                    attr.getImportSetter().execute(p, currentTestCase, rawValue);
                }
            }

            result.add(currentTestCase);
        }

        return result;
    }

    /**
     * Quote-aware CSV parser over the whole character stream. Unlike a per-line
     * parser, this keeps newlines inside quoted fields (which our own CSV export
     * produces for multi-line steps), reads UTF-8 explicitly, and strips a BOM.
     */
    private @NotNull List<String[]> parseCsvRecords(final @NotNull File file) {
        final @NotNull List<String[]> records = new ArrayList<>();
        final @NotNull List<String> fields = new ArrayList<>();
        final @NotNull StringBuilder current = new StringBuilder();

        try (PushbackReader reader = new PushbackReader(
                new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))) {

            boolean inQuotes = false;
            boolean firstChar = true;
            int ci;
            while ((ci = reader.read()) != -1) {
                final char c = (char) ci;

                if (firstChar) {
                    firstChar = false;
                    if (c == '\ufeff') continue; // BOM
                }

                if (inQuotes) {
                    if (c == '"') {
                        final int next = reader.read();
                        if (next == '"') {
                            current.append('"'); // escaped quote
                        } else {
                            inQuotes = false;
                            if (next != -1) reader.unread(next);
                        }
                    } else {
                        current.append(c);
                    }
                } else if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else if (c == '\r' || c == '\n') {
                    if (c == '\r') {
                        final int next = reader.read();
                        if (next != '\n' && next != -1) reader.unread(next);
                    }
                    endRecord(records, fields, current);
                } else {
                    current.append(c);
                }
            }

            endRecord(records, fields, current);

        } catch (final IOException ex) {
            Logger.error("CSV parse failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return records;
    }

    private void endRecord(final @NotNull List<String[]> records, final @NotNull List<String> fields,
                           final @NotNull StringBuilder current) {
        if (fields.isEmpty() && current.isEmpty()) return; // blank line

        fields.add(current.toString());
        current.setLength(0);

        if (!fields.stream().allMatch(String::isEmpty)) {
            records.add(fields.toArray(new String[0]));
        }
        fields.clear();
    }
}
