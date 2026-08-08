package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

@FunctionalInterface
public interface TestValueExtractor {
    @NotNull String execute(final @NotNull TestCaseDto tc, final @NotNull Project p);
}
