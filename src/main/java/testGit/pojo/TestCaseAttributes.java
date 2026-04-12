package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.pojo.dto.TestCaseDto;

import java.util.Optional;
import java.util.function.Function;

@Getter
@AllArgsConstructor
/// TODO: add order, then add it toolbar details (select by the order number) & add it to edit menu.
/// TODO: add all to edit menu: auto ref, business ref..etc
///  TODO: also, map all to view panel dynamically.
/// TODO: may you need to unify enum map to map all with one source of truth
public enum TestCaseAttributes {

    ID("ID",
            true,
            true,
            tc -> String.valueOf(tc.getId())
    ),

    TITLE("Title",
            false,
            false,
            TestCaseDto::getTitle
    ),

    EXPECTED_RESULT("Expected Result",
            true,
            true,
            TestCaseDto::getExpected
    ),

    STEPS("Steps",
            true,
            true,
            tc -> Optional.ofNullable(tc.getSteps()).map(Object::toString).orElse(null)
    ),

    PRIORITY("Priority",
            false,
            true,
            tc -> null
    ),

    AUTO_REF("Automation Referrence",
            true,
            true,
            TestCaseDto::getAutoRef
    ),

    BUSI_REF("Business Referrence",
            true,
            true,
            TestCaseDto::getBusiRef
    ),

    GROUPS("Groups",
            false,
            true,
            tc -> null
    ),

    CREATE_BY("Created By",
            false,
            false,
            tc -> null
    ),

    UPDATE_BY("Updated By",
            false,
            false,
            tc -> null
    ),

    CREATE_AT("Created At",
            false,
            false,
            tc -> null
    ),

    UPDATE_AT("Updated At",
            false,
            false,
            tc -> null
    ),

    MODULE("Module",
            true,
            true,
            TestCaseDto::getModule
    ),

    APPROVAL_STATUS(
            "Approval Status",
            false,
            false,
            tc -> null
    );

    private final String displayName;
    private final boolean standardOption;
    private final boolean defaultSelected;

    private final Function<TestCaseDto, String> valueExtractor;

    public String getValue(final TestCaseDto tc) {
        try {
            return valueExtractor.apply(tc);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}