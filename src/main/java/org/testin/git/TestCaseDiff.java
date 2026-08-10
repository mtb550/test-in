package org.testin.git;

import org.testin.mappers.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;

public record TestCaseDiff(String testCaseId, Path relativeFilePath, DiffType type, TestCaseDto oldState,
                           TestCaseDto newState, List<FieldChange> fieldChanges) {
}
