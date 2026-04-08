package testGit.ui.TestCase.edit.bulk;

import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.persist.PersistenceManager;

import java.util.List;

public class TitleBulkEditor extends JsonSplitBulkEditor {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Titles (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getOriginalValue(TestCaseDto tc) {
        return tc.getTitle();
    }

    @Override
    protected void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
        String id = "Item-" + (index + 1);
        String escapedTitle = escapeJson(tc.getTitle());

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
    protected void saveValues(List<TestCaseDto> items, List<String> newValues, Runnable onUpdate) {
        PersistenceManager.getInstance(Config.getProject()).updateTitles(items, newValues.toArray(new String[0]), onUpdate);
    }
}