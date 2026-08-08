package org.testin.git;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChangeType {
    CREATE_TEST_CASE("Create Test Case"),
    REMOVE_TEST_CASE("Remove Test Case"),
    CHANGE_DESCRIPTION("Change Description"),
    CHANGE_EXPECTED_RESULT("Change Expected Result"),
    CHANGE_PRIORITY("Change Priority"),
    CHANGE_GROUP("Change Group");

    private final String label;
}
