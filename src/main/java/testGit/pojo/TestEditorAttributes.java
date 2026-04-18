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
public enum TestEditorAttributes {

    ID("ID",
            true,
            false,
            tc -> String.valueOf(tc.getId())
    ),

    /// TODO:: added to tool bar details, to be shown but disabled
    DESCRIPTION("Description",
            false,
            false,
            TestCaseDto::getDescription
    ),

    EXPECTED_RESULT("Expected Result",
            true,
            true,
            TestCaseDto::getExpectedResult
    ),

    STEPS("Steps",
            true,
            true,
            tc -> Optional.of(tc.getSteps()).map(Object::toString).orElse("")
    ),

    PRIORITY("Priority",
            true,
            true,
            tc -> Optional.of(tc.getPriority()).map(Priority::getName).orElse("")
    ),

    FCQN("FCQN",
            true,
            false,
            TestCaseDto::getFqcn
    ),

    REFERRENCE("Referrence",
            true,
            false,
            TestCaseDto::getReference
    ),

    GROUP("Group",
            true,
            true,
            tc -> Optional.of(tc.getGroup()).map(groups -> groups.stream().map(Group::getName).collect(Collectors.joining(", "))).orElse("")
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

    STATUS(
            "Status",
            true,
            false,
            tc -> null
    );

    private final String name;
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