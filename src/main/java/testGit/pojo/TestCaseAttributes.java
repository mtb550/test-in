package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.pojo.dto.TestCaseDto;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
/// TODO: add order, then add it toolbar details (select by the order number) & add it to edit menu.
/// TODO: add all to edit menu: auto ref, business ref..etc
///  TODO: also, map all to view panel dynamically.
/// TODO: may you need to unify enum map to map all with one source of truth
public enum TestCaseAttributes {

    ID("ID",
            true,
            false,
            tc -> String.valueOf(tc.getId())
    ),

    TITLE("Title",      /// TODO:: added to tool bar details, to be shown but disabled
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
            tc -> Optional.ofNullable(tc.getSteps()).map(Object::toString).orElse("")
    ),

    PRIORITY("Priority",
            true,
            true,
            tc -> Optional.ofNullable(tc.getPriority()).map(Priority::getName).orElse("")
    ),

    AUTO_REF("Automation Referrence",
            true,
            false,
            TestCaseDto::getAutoRef
    ),

    BUSI_REF("Business Referrence",
            true,
            false,
            TestCaseDto::getBusiRef
    ),

    GROUPS("Groups",
            true,
            true,
            tc -> Optional.ofNullable(tc.getGroups()).map(groups -> groups.stream().map(Groups::getName).collect(Collectors.joining(", "))).orElse("")
    ),

    ///  TODO:: ORDER to be added to show or hide sequence numbers in editors

    CREATE_BY("Created By",
            true,
            false,
            tc -> null
    ),

    UPDATE_BY("Updated By",
            true,
            false,
            tc -> null
    ),

    CREATE_AT("Created At",
            true,
            false,
            tc -> null
    ),

    UPDATE_AT("Updated At",
            true,
            false,
            tc -> null
    ),

    MODULE("Module",
            true,
            false,
            TestCaseDto::getModule
    ),

    APPROVAL_STATUS(
            "Approval Status",
            true,
            false,
            tc -> null
    );

    private final String displayName;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final Function<TestCaseDto, String> valueExtractor;

    public String getValue(final TestCaseDto tc) {
        try {
            return valueExtractor.apply(tc);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}