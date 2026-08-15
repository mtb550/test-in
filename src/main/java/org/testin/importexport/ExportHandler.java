package org.testin.importexport;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.exports.ExportAction;
import org.testin.model.dto.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ExportHandler {
    /**
     * The formats that cannot be exported to. A value rather than a null, so
     * {@link org.testin.importexport.FileTypes} holds a handler either way and the
     * dropdowns ask what a format supports instead of whether one exists.
     */
    ExportHandler UNSUPPORTED = (p, exportAction, destFile, sheetsData) -> {
        throw new IllegalStateException("This format cannot be exported to");
    };

    void execute(final @NotNull Project p, final @NotNull ExportAction exportAction, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData);
}
