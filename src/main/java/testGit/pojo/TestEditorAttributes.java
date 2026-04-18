package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.editorPanel.Shared;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
/// TODO: add order, then add it toolbar details (select by the order number) & add it to edit menu.
/// TODO: add all to edit menu: auto ref, business ref..etc
///  TODO: also, map all to view panel dynamically.
/// TODO: may you need to unify enum map to map all with one source of truth
@AllArgsConstructor
public enum TestEditorAttributes {

    ID("ID",
            true,
            false,
            tc -> String.valueOf(tc.getId()),
            null
    ),

    /// TODO:: added to tool bar details, to be shown but disabled
    DESCRIPTION("Description",
            true,
            true,
            TestCaseDto::getDescription,
            null
    ),

    EXPECTED_RESULT("Expected Result",
            true,
            true,
            TestCaseDto::getExpectedResult,
            null
    ),

    STEPS("Steps",
            true,
            true,
            tc -> String.join(", ", tc.getSteps()),
            null
    ),

    PRIORITY("Priority",
            true,
            true,
            tc -> tc.getPriority().getName(),
            tc -> List.of(Shared.createPriorityBadge(tc))
    ),

    FCQN("FCQN",
            true,
            false,
            TestCaseDto::getFqcn,
            null
    ),

    REFERRENCE("Referrence",
            true,
            false,
            TestCaseDto::getReference,
            null
    ),

    GROUP("Group",
            true,
            true,
            tc -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")), // تم إزالة Optional
            tc -> tc.getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList())
    ),

    ///  TODO:: ORDER to be added to show or hide sequence numbers in editors

    CREATE_BY("Created By",
            true,
            false,
            TestCaseDto::getCreatedBy,
            null
    ),

    UPDATE_BY("Updated By",
            true,
            false,
            TestCaseDto::getUpdatedBy, 
            null
    ),

    CREATE_AT("Created At",
            true,
            false,
            TestCaseDto::getFormattedCreatedAt,
            null
    ),

    UPDATE_AT("Updated At",
            true,
            false,
            TestCaseDto::getFormattedUpdatedAt, 
            null
    ),

    MODULE("Module",
            true,
            false,
            TestCaseDto::getModule,
            null
    ),

    STATUS("Status",
            true,
            false,
            TestCaseDto::getTempStatus,
            null
    );

    private final String name;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;

    private final Function<TestCaseDto, String> valueExtractor;
    private final Function<TestCaseDto, List<JComponent>> drawItem;

    public String getValue(final TestCaseDto tc) {
        try {
            return valueExtractor.apply(tc);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    //todo, to be removed, use runnable
    public void applyToUI(final TestCaseDto tc, final List<JComponent> badges, final Map<String, String> details) {
        if (drawItem != null) {
            badges.addAll(drawItem.apply(tc));
        } else {
            details.put(name, getValue(tc));
        }
    }
}