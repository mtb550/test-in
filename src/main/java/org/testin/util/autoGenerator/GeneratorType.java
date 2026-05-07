package org.testin.util.autoGenerator;

import lombok.Getter;

@Getter
public enum GeneratorType {
    CREATE_TEST_PROJECT(
            "Create Test Project",
            "Create Automation Test Project",
            new CreateTestProject()
    ),

    CREATE_TEST_SET(
            "Create Test Set",
            "Create Automation Test Class",
            new CreateTestSet()
    ),

    CREATE_TEST_SET_PACKAGE(
            "Create Test Set Package",
            "Create Automation Test Package",
            new CreateTestSetPackage()
    ),

    CREATE_TEST_CASE(
            "Create Test Case",
            "Create Automation Test Method",
            new CreateTestCase()
    ),

    UPDATE_TEST_CASE_Description(
            "Update Test Case",
            "Update Automation Test Method Description & Name",
            new UpdateTestCase()
    ),

    UPDATE_TEST_CASE_EXPECTED_RESULT(
            "Update Test Case",
            "Update Automation Test Method Expected Result",
            new UpdateTestCase()
    ),

    UPDATE_TEST_CASE_STEPS(
            "Update Test Case",
            "Update Automation Test Method Steps",
            new UpdateTestCase()
    ),

    UPDATE_TEST_CASE_GROUP(
            "Update Test Case",
            "Update Automation Test Method Group",
            new UpdateTestCase()
    ),

    UPDATE_TEST_CASE_PRIORITY(
            "Update Test Case",
            "Update Automation Test Method Priority",
            new UpdateTestCase()
    ),

    UPDATE_TEST_CASE_ORDER(
            "Update Test Case",
            "Update Automation Test Method Order",
            new UpdateTestCase()
    );

    private final String description;
    private final String tooltip;
    private final GeneratorAction action;

    GeneratorType(final String description, final String tooltip, final GeneratorAction action) {
        this.description = description;
        this.tooltip = tooltip;
        this.action = action;
    }
}