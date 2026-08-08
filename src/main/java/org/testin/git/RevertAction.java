package org.testin.git;

import org.testin.mappers.dto.TestCaseDto;

@FunctionalInterface
public interface RevertAction {
    void apply(final TestCaseDto currentDto, final TestCaseDto oldDto);
}
