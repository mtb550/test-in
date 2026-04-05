package testGit.ui.editTestCase.bulk;

import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;

import java.util.List;

public class ExpectedBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        JsonSplitBulkEditor.show(selectedItems, onUpdate, new JsonSplitBulkEditor.JsonFieldConfig() {
            @Override
            public String getPopupTitle() {
                return "Bulk Edit Expected Results (Enter to Save | Tab/Arrows to Navigate)";
            }

            @Override
            public String getOriginalValue(TestCaseDto tc) {
                return tc.getExpected();
            }

            @Override
            public void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
                String id = "Item-" + (index + 1);
                String escapedTitle = JsonSplitBulkEditor.escapeJson(tc.getTitle());
                String escapedExpected = JsonSplitBulkEditor.escapeJson(tc.getExpected());

                String prefix = "  {\n    \"id\": \"" + id + "\",\n    \"title\": \"" + escapedTitle + "\",\n    \"expected\": \"";
                String suffix = "\"\n  }";
                String comma = isLast ? "\n" : ",\n";

                leftSb.append(prefix).append(escapedExpected).append(suffix).append(comma);

                rightSb.append(prefix);
                int startOffset = rightSb.length();
                rightSb.append(escapedExpected);
                int endOffset = rightSb.length();
                rightEditableRanges.add(new int[]{startOffset, endOffset});
                rightSb.append(suffix).append(comma);
            }

            @Override
            public void saveValues(List<TestCaseDto> items, List<String> newValues, Runnable onUpdate) {
                PersistenceManager.updateExpected(items, newValues.toArray(new String[0]), onUpdate);
            }
        });
    }
}