package org.testin.actions.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.io.File;
import java.util.*;

public class ImportJson {

    public Map<String, List<TestCaseDto>> processImport(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            Map<String, List<TestCaseDto>> parsed = parseFile(project, file);
            result.putAll(parsed);
        } catch (final Exception ex) {
            Log.error("JSON import parse failed: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "JSON Parse Error", ex.getMessage());
        }
        return result;
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> data = Services.getInstance(project, Mapper.class).readValue(file, new TypeReference<>() {
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