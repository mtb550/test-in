package org.testin.git;

/**
 * A single changed field inside a {@link TestCaseDiff}.
 */
public record FieldChange(String fieldName, String oldValue, String newValue, ChangeType changeType) {
}
