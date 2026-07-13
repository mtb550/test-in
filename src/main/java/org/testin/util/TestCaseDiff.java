package org.testin.util;

import org.testin.pojo.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;

public record TestCaseDiff(String testCaseId, Path relativeFilePath, DiffType type, TestCaseDto oldState,
                           TestCaseDto newState, List<FieldChange> fieldChanges) {
    public enum DiffType {ADDED, MODIFIED, DELETED}

    /**
     * Describes what kind of change was made to a test case field.
     */
    public enum ChangeType {
        CREATE_TEST_CASE,
        REMOVE_TEST_CASE,
        CHANGE_DESCRIPTION,
        CHANGE_EXPECTED_RESULT,
        CHANGE_PRIORITY,
        CHANGE_GROUP
    }

    public record FieldChange(String fieldName, String oldValue, String newValue, ChangeType changeType) {
    }
}
