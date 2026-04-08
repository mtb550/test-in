package testGit.ui.TestCase.edit.bulk;

import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.persist.PersistenceManager;

import java.util.List;

public class ExpectedBulkEditor extends JsonSplitBulkEditor {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Expected Results (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getOriginalValue(TestCaseDto tc) {
        return tc.getExpected();
    }

    @Override
    protected void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
        String id = "Item-" + (index + 1);
        String escapedTitle = escapeJson(tc.getTitle());
        String escapedExpected = escapeJson(tc.getExpected());

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
    protected void saveValues(List<TestCaseDto> items, List<String> newValues, Runnable onUpdate) {
        PersistenceManager.getInstance(Config.getProject()).updateExpected(items, newValues.toArray(new String[0]), onUpdate);
    }
}