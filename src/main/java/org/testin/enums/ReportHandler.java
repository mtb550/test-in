package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;

import java.util.Map;
import java.util.UUID;

@FunctionalInterface
public interface ReportHandler {
    byte @NotNull [] execute(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                             final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap);
}
