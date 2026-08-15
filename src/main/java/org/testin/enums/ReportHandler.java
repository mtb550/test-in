package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;

import java.util.Map;
import java.util.UUID;

@FunctionalInterface
public interface ReportHandler {
    /**
     * The formats with no report generator — see {@link ExportHandler#UNSUPPORTED}.
     */
    ReportHandler UNSUPPORTED = (p, trDir, tr, detailsMap) -> {
        throw new IllegalStateException("This format has no report generator");
    };

    byte @NotNull [] execute(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                             final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap);
}
