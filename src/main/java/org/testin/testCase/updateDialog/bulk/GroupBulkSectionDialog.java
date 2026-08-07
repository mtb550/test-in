package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Group;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

public class GroupBulkSectionDialog extends JsonArraySplitBulkSectionDialog {

    public GroupBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Group";
    }

    @Override
    protected String getArrayFieldName() {
        return "Group";
    }

    @Override
    protected List<List<String>> extractOriginalValues(final List<TestCaseDto> items) {
        List<List<String>> originalGroups = new ArrayList<>();

        for (TestCaseDto tc : items) {
            List<String> groupStrings = new ArrayList<>();
            for (Group g : tc.getGroup()) {
                groupStrings.add(g.name());
            }
            originalGroups.add(groupStrings);
        }

        return originalGroups;
    }

    @Override
    protected void applyValues(final List<TestCaseDto> items, final List<List<String>> newValues) {
        for (int i = 0; i < items.size(); i++) {
            List<Group> enumList = new ArrayList<>();

            for (String str : newValues.get(i)) {
                if (str == null) continue;
                String cleanStr = str.trim();

                if (!cleanStr.isEmpty()) {
                    try {
                        Group g = Group.valueOf(cleanStr.toUpperCase());
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