package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.Group;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GroupBulkSectionDialog extends JsonArraySplitBulkSectionDialog {

    public GroupBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems,
                                   final @Nullable Consumer<List<TestCaseDto>> updatedItems) {
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
        final List<List<String>> originalGroups = new ArrayList<>();

        for (final TestCaseDto tc : items) {
            final List<String> groupStrings = new ArrayList<>();
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
            final List<Group> enumList = new ArrayList<>();

            for (final String str : newValues.get(i)) {
                if (str == null) continue;
                final String cleanStr = str.trim();

                if (!cleanStr.isEmpty()) {
                    try {
                        final Group g = Group.valueOf(cleanStr.toUpperCase());
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