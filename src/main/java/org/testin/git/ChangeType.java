package org.testin.git;

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
