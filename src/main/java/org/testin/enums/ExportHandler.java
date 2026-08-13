package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importExport.exports.ExportAction;
import org.testin.mappers.dto.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ExportHandler {
    void execute(final @NotNull Project p, final @NotNull ExportAction exportAction, final @NotNull File destFile,
                 final @NotNull Map<String, List<TestCaseDto>> sheetsData);
}
