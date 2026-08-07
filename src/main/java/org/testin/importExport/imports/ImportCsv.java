package org.testin.importExport.imports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ImportCsv {
    private final ImportAction importAction;

    public ImportCsv(final @NotNull ImportAction importAction) {
        this.importAction = importAction;
    }

    public Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            List<TestCaseDto> testCases = parseFile(p, file);
            if (!testCases.isEmpty()) {
                String name = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
                result.put(name, testCases);
            }
        } catch (final Exception ex) {
            Logger.error("CSV import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "CSV Parse Error", ex.getMessage());
        }
        return result;
    }

    public List<TestCaseDto> parseFile(final @NotNull Project p, final File file) {
        return parseCsvFile(file, p);
    }

    private List<TestCaseDto> parseCsvFile(final File file, final @NotNull Project p) {
        List<TestCaseDto> result = new ArrayList<>();
        List<String[]> records = parseCsvRecords(file);

        if (records.isEmpty()) return result;

        String[] headers = records.getFirst();
        Map<String, Integer> headerIndexMap = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String headerName = headers[i].trim();
            for (TestEditorAttributes reqCol : importAction.importAttributes) {
                if (reqCol.getName().equalsIgnoreCase(headerName)) {
                    headerIndexMap.put(reqCol.getName().toLowerCase(), i);
                }
            }
        }

        for (int r = 1; r < records.size(); r++) {
            String[] values = records.get(r);

            boolean isRowEmpty = true;
            for (String val : values) {
                if (val != null && !val.trim().isEmpty()) {
                    isRowEmpty = false;
                    break;
                }
            }
            if (isRowEmpty) continue;

            final TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());
            currentTestCase.setNext(null);
            currentTestCase.setIsHead(null);

            for (TestEditorAttributes attr : TestEditorAttributes.values()) {
                if (attr.isImportable()) {
                    Integer colIndex = headerIndexMap.get(attr.getName().toLowerCase());
                    String rawValue = "";
                    if (colIndex != null && colIndex < values.length) {
                        String val = values[colIndex];
                        rawValue = val != null ? val.trim() : "";
                    }
                    attr.getImportSetter().accept(p, currentTestCase, rawValue);
                }
            }

            result.add(currentTestCase);
        }

        return result;
    }

    private List<String[]> parseCsvRecords(final File file) {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (records.isEmpty() && line.charAt(0) == '\ufeff') {
                    line = line.substring(1);
                }

                String[] fields = parseCsvLine(line);
                boolean allEmpty = true;
                for (String f : fields) {
                    if (f != null && !f.isEmpty()) {
                        allEmpty = false;
                        break;
                    }
                }
                if (!allEmpty) {
                    records.add(fields);
                }
            }
        } catch (final IOException ex) {
            Logger.error("CSV parse failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return records;
    }

    private String[] parseCsvLine(final String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    inQuotes = true;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}