package org.testin.importExport.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.io.File;
import java.util.*;

public class ImportJson {

    public Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            Map<String, List<TestCaseDto>> parsed = parseFile(p, file);
            result.putAll(parsed);
        } catch (final Exception ex) {
            Logger.error("JSON import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "JSON Parse Error", ex.getMessage());
        }
        return result;
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project p, final File file) {
        Map<String, List<TestCaseDto>> data = Services.getInstance(p, Mapper.class).readValue(file, new TypeReference<>() {
        });
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<TestCaseDto>> entry : data.entrySet()) {
            List<TestCaseDto> sanitized = new ArrayList<>();

            for (TestCaseDto tc : entry.getValue()) {
                tc.setId(UUID.randomUUID());
                tc.setIsHead(null);
                tc.setNext(null);
                sanitized.add(tc);
            }

            if (!sanitized.isEmpty()) {
                result.put(entry.getKey(), sanitized);
            }
        }
        return result;
    }
}