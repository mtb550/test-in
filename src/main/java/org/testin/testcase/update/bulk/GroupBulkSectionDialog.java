package org.testin.testcase.update.bulk;

import org.testin.model.TestEditorAttributes;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.TestDataParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class GroupBulkSectionDialog extends JsonArraySplitBulkSectionDialog {

    public GroupBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Group";
    }

    @Override
    protected @NotNull String getArrayFieldName() {
        return TestEditorAttributes.GROUP.getName();
    }

    @Override
    protected @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items) {
        final @NotNull List<List<String>> originalGroups = new ArrayList<>();

        for (final TestCaseDto tc : items) {
            final @NotNull List<String> groupStrings = new ArrayList<>();
            for (final Group g : tc.getGroup()) {
                groupStrings.add(g.name());
            }
            originalGroups.add(groupStrings);
        }

        return originalGroups;
    }

    /**
     * UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-206.
     * <p>
     * Writes the groups the tester typed, and says how many rows Testin could
     * not read.
     * <p>
     * This was a fourth reader of the same text, with its own {@code valueOf},
     * its own upper-casing and its own deduplication - so it took the constant
     * name where every other surface takes the label the tester is looking at,
     * and a group it could not read went to the log and nowhere else. A tester
     * who mistyped one group in a list of four got three back and no sign the
     * fourth had gone (#264, #295).
     */
    @Override
    protected void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues) {
        int refused = 0;

        for (int i = 0; i < items.size(); i++) {
            final @NotNull Optional<List<Group>> read = TestDataParser.groups(String.join(",", newValues.get(i)));

            if (read.isEmpty()) {
                refused++;
                continue;
            }

            items.get(i).setGroup(read.get());
        }

        TestEditorAttributes.sayWhatWasRefused(p, refused);
    }
}