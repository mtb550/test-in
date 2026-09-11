package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.Groups;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GroupBulkSectionDialog extends JsonArraySplitBulkSectionDialog {

    public GroupBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.group");
    }

    @Override
    protected @NotNull String getArrayFieldName() {
        return TestEditorAttributes.GROUP.getName();
    }

    @Override
    protected @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items) {
        return items.stream().map(tc -> (List<String>) new ArrayList<>(tc.getGroup())).toList();
    }

    /**
     * UC-EDITOR-PANEL-007.
     * <p>
     * Writes the groups the tester typed.
     * <p>
     * Nothing here can be refused any more. This was a fourth reader of the same
     * text, with its own {@code valueOf}, its own upper-casing and its own
     * deduplication, so it took the constant name where every other surface took
     * the label - and a group it could not read went to the log and nowhere else
     * (#264, #295). It asks {@link Groups} now, and a group Testin has never
     * seen is a group the tester is adding (#296).
     */
    @Override
    protected void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setGroup(Groups.read(String.join(",", newValues.get(i))));
        }
    }

}