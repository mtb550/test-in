package testGit.ui.TestCase.update.bulk;

import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

public class GroupsBulkSection extends JsonArraySplitBulkSection {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Groups";
    }

    @Override
    protected String getArrayFieldName() {
        return "groups";
    }

    @Override
    protected List<List<String>> extractOriginalValues(final List<TestCaseDto> items) {
        List<List<String>> originalGroups = new ArrayList<>();

        for (TestCaseDto tc : items) {
            List<String> groupStrings = new ArrayList<>();
            if (tc.getGroups() != null) {
                for (Groups g : tc.getGroups()) {
                    groupStrings.add(g.name());
                }
            }
            originalGroups.add(groupStrings);
        }

        return originalGroups;
    }

    @Override
    protected void applyValues(final List<TestCaseDto> items, final List<List<String>> newValues) {
        for (int i = 0; i < items.size(); i++) {
            List<Groups> enumList = new ArrayList<>();

            for (String str : newValues.get(i)) {
                if (str == null) continue;
                String cleanStr = str.trim();

                if (!cleanStr.isEmpty()) {
                    try {
                        Groups g = Groups.valueOf(cleanStr.toUpperCase());
                        if (!enumList.contains(g)) {
                            enumList.add(g);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            items.get(i).setGroups(enumList.isEmpty() ? null : enumList);
        }
    }
}