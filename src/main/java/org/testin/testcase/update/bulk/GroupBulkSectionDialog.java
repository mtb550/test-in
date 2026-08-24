package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        return "Group";
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

    @Override
    protected void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues) {
        for (int i = 0; i < items.size(); i++) {
            final @NotNull List<Group> enumList = new ArrayList<>();

            for (final String str : newValues.get(i)) {
                final @NotNull String cleanStr = Objects.toString(str, "").trim();

                if (!cleanStr.isEmpty()) {
                    try {
                        final @NotNull Group g = Group.valueOf(cleanStr.toUpperCase());
                        if (!enumList.contains(g)) {
                            enumList.add(g);
                        }
                    } catch (final IllegalArgumentException ex) {
                        Logger.error(ex.getMessage());
                    }
                }
            }

            items.get(i).setGroup(enumList);
        }
    }
}