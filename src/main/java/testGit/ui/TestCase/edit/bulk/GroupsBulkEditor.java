package testGit.ui.TestCase.edit.bulk;

import testGit.pojo.Config;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.persist.PersistenceManager;

import java.util.ArrayList;
import java.util.List;

public class GroupsBulkEditor extends JsonArraySplitBulkEditor {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Groups";
    }

    @Override
    protected String getArrayFieldName() {
        return "groups";
    }

    @Override
    protected List<List<String>> extractOriginalValues(List<TestCaseDto> items) {
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
    protected void saveValues(List<TestCaseDto> items, List<List<String>> activeValues, Runnable onUpdate) {
        List<List<Groups>> newGroupsList = new ArrayList<>();

        for (List<String> stringList : activeValues) {
            List<Groups> enumList = new ArrayList<>();

            for (String str : stringList) {
                String cleanStr = str.trim();
                if (!cleanStr.isEmpty()) {
                    for (Groups g : Groups.values()) {
                        if (g.name().equalsIgnoreCase(cleanStr)) {
                            if (!enumList.contains(g)) {
                                enumList.add(g);
                            }
                            break;
                        }
                    }
                }
            }
            newGroupsList.add(enumList);
        }

        PersistenceManager.getInstance(Config.getProject()).updateGroups(items, newGroupsList, onUpdate);
    }
}