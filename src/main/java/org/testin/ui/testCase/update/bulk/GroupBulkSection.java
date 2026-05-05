package org.testin.ui.testCase.update.bulk;

import org.testin.pojo.Group;
import org.testin.pojo.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

public class GroupBulkSection extends JsonArraySplitBulkSection {

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
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            items.get(i).setGroup(enumList);
        }
    }
}