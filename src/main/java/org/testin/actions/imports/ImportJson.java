package org.testin.actions.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.services.Services;

import java.io.File;
import java.util.*;

public class ImportJson extends ImportBase {

    public ImportJson(final @NotNull SimpleTree tree) {
        super(tree, "Import from JSON", "Import test cases from a JSON file", AllIcons.FileTypes.Json);
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> data = Services.getInstance(project, Mapper.class).readValue(file, new TypeReference<>() {
        });
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        if (data != null) {
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
        }
        return result;
    }
}
