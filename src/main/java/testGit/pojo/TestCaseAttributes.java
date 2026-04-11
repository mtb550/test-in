package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestCaseAttributes {

    ID("ID"),

    TITLE("Title"),

    EXPECTED_RESULT("Expected Result"),

    STEPS("Steps"),

    PRIORITY("Priority"),

    AUTO_REF("Automation Referrence"),

    BUSI_REF("Business Referrence"),

    GROUPS("Groups"),

    CREATE_BY("Created By"),

    UPDATE_BY("Updated By"),

    CREATE_AT("Created At"),

    UPDATE_AT("Updated At"),

    MODULE("Module"),

    APPROVAL_STATUS("Approval Status");


    private final String displayName;
}