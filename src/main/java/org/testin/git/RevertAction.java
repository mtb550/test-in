package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

@FunctionalInterface
public interface RevertAction {
    void apply(final @NotNull TestCaseDto currentDto, final @NotNull TestCaseDto oldDto);
}
