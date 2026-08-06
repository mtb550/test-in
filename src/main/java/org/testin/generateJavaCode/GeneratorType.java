package org.testin.generateJavaCode;

import lombok.Getter;
import org.testin.generateJavaCode.clazz.CreateJavaClass;
import org.testin.generateJavaCode.clazz.RemoveJavaClass;
import org.testin.generateJavaCode.clazz.RenameJavaClass;
import org.testin.generateJavaCode.method.CreateTestMethod;
import org.testin.generateJavaCode.method.RemoveTestMethod;
import org.testin.generateJavaCode.method.RenameTestMethod;
import org.testin.generateJavaCode.method.update.*;
import org.testin.generateJavaCode.pkg.CreateJavaPackage;
import org.testin.generateJavaCode.pkg.RemoveJavaPackage;
import org.testin.generateJavaCode.pkg.RenameJavaPackage;

@Getter
public enum GeneratorType {
    CREATE_TEST_PROJECT(
            "Create Test Project",
            "Create Automation Test Project",
            new CreateJavaPackage()
    ),

    REMOVE_TEST_PROJECT(
            "Remove Test Project",
            "Remove Automation Test Project",
            new RemoveJavaPackage()
    ),

    RENAME_TEST_PROJECT(
            "Rename Test Project",
            "Rename Automation Test Project",
            new RenameJavaPackage()
    ),

    CREATE_JAVA_PACKAGE(
            "Create Test Set Package",
            "Create Automation Test Package",
            new CreateJavaPackage()
    ),

    REMOVE_JAVA_PACKAGE(
            "Remove Test Set Package",
            "Remove Automation Test Package",
            new RemoveJavaPackage()
    ),

    RENAME_JAVA_PACKAGE(
            "Rename Test Set Package",
            "Rename Automation Test Package",
            new RenameJavaPackage()
    ),

    CREATE_JAVA_CLASS(
            "Create Test Set",
            "Create Automation Test Class",
            new CreateJavaClass()
    ),

    REMOVE_JAVA_CLASS(
            "Remove Test Set",
            "Remove Automation Test Class",
            new RemoveJavaClass()
    ),

    RENAME_JAVA_CLASS(
            "Rename Test Set",
            "Rename Automation Test Class",
            new RenameJavaClass()
    ),

    CREATE_TEST_METHOD(
            "Create Test Case",
            "Create Automation Test Method",
            new CreateTestMethod()
    ),

    REMOVE_TEST_METHOD(
            "Remove Test Case",
            "Remove Automation Test Method",
            new RemoveTestMethod()
    ),

    RENAME_TEST_METHOD(
            "Rename Test Case",
            "Rename Automation Test Method",
            new RenameTestMethod()
    ),

    UPDATE_TEST_CASE_DESCRIPTION(
            "Update Test Case",
            "Update Automation Test Method Description & Name",
            new UpdateTestDescription()
    ),

    UPDATE_TEST_CASE_EXPECTED_RESULT(
            "Update Test Case",
            "Update Automation Test Method Expected Result",
            new UpdateTestExpectedResult()
    ),

    UPDATE_TEST_CASE_MODULE(
            "Update Test Case",
            "Update Automation Test Method Groups",
            new UpdateTestModule()
    ),

    UPDATE_TEST_CASE_TEST_DATA(
            "Update Test Case",
            "Update Automation Test Method Test Data",
            new UpdateTestTestData()
    ),

    UPDATE_TEST_CASE_PRE_CONDITIONS(
            "Update Test Case",
            "Update Automation Test Method Pre Conditions",
            new UpdateTestPreCondition()
    ),

    UPDATE_TEST_CASE_STEPS(
            "Update Test Case",
            "Update Automation Test Method Steps",
            new UpdateTestSteps()
    ),

    UPDATE_TEST_CASE_GROUP(
            "Update Test Case",
            "Update Automation Test Method Group",
            new UpdateTestGroup()
    ),

    UPDATE_TEST_CASE_PRIORITY(
            "Update Test Case",
            "Update Automation Test Method Priority",
            new UpdateTestPriority()
    ),

    UPDATE_TEST_CASE_ORDER(
            "Update Test Case",
            "Update Automation Test Method Order",
            new UpdateTestOrder()
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