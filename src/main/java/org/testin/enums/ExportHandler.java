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
    /**
     * The formats that cannot be exported to. A value rather than a null, so
     * {@link org.testin.enums.FileTypes} holds a handler either way and the
     * dropdowns ask what a format supports instead of whether one exists.
     */
    ExportHandler UNSUPPORTED = (p, exportAction, destFile, sheetsData) -> {
        throw new IllegalStateException("This format cannot be exported to");
    };

    void execute(final @NotNull Project p, final @NotNull ExportAction exportAction, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData);
}
