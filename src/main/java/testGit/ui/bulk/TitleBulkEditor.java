package testGit.ui.bulk;

import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;

import java.util.List;

public class TitleBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        JsonSplitBulkEditor.show(selectedItems, onUpdate, new JsonSplitBulkEditor.JsonFieldConfig() {
            @Override
            public String getPopupTitle() {
                return "Bulk Edit Titles (Enter to Save | Tab/Arrows to Navigate)";
            }

            @Override
            public String getOriginalValue(TestCaseDto tc) {
                return tc.getTitle();
            }

            @Override
            public void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
                String id = "Item-" + (index + 1);
                String escapedTitle = JsonSplitBulkEditor.escapeJson(tc.getTitle());

                String prefix = "  {\n    \"id\": \"" + id + "\",\n    \"title\": \"";
                String suffix = "\"\n  }";
                String comma = isLast ? "\n" : ",\n";

                leftSb.append(prefix).append(escapedTitle).append(suffix).append(comma);

                rightSb.append(prefix);
                int startOffset = rightSb.length();
                rightSb.append(escapedTitle);
                int endOffset = rightSb.length();
                rightEditableRanges.add(new int[]{startOffset, endOffset});
                rightSb.append(suffix).append(comma);
            }

            @Override
            public void saveValues(List<TestCaseDto> items, List<String> newValues, Runnable onUpdate) {
                PersistenceManager.updateTitles(items, newValues.toArray(new String[0]), onUpdate);
            }
        });
    }
}