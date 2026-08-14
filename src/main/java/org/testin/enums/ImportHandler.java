package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importExport.imports.ImportAction;
import org.testin.mappers.dto.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ImportHandler {
    /**
     * The formats that cannot be imported from — see {@link ExportHandler#UNSUPPORTED}.
     */
    ImportHandler UNSUPPORTED = (p, importAction, importFile) -> {
        throw new IllegalStateException("This format cannot be imported from");
    };

    @NotNull Map<String, List<TestCaseDto>> execute(final @NotNull Project p, final @NotNull ImportAction importAction, final @NotNull File importFile);
}
