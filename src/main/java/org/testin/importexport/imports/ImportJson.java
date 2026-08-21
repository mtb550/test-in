package org.testin.importexport.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.File;
import java.util.*;

public class ImportJson {

    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            result.putAll(parseFile(p, file));
        } catch (final Exception ex) {
            Logger.error("JSON import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "JSON Parse Error", ex.getMessage());
        }
        return result;
    }

    public @NotNull Map<String, List<TestCaseDto>> parseFile(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> data = Services.getInstance(p, Mapper.class).readValue(file, new TypeReference<>() {
        });
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        for (final Map.Entry<String, List<TestCaseDto>> entry : data.entrySet()) {
            final @NotNull List<TestCaseDto> sanitized = new ArrayList<>();

            for (final TestCaseDto tc : entry.getValue()) {
                tc.setId(UUID.randomUUID());
                sanitized.add(tc);
            }

            if (!sanitized.isEmpty()) {
                result.put(entry.getKey(), sanitized);
            }
        }
        return result;
    }
}