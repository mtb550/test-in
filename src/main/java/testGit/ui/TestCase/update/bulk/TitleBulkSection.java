package testGit.ui.TestCase.update.bulk;

import testGit.pojo.dto.TestCaseDto;

import java.util.List;

public class TitleBulkSection extends JsonSplitBulkSection {

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
    protected void applyValues(final List<TestCaseDto> items, final List<String> newValues) {
        for (int i = 0; i < items.size(); i++) {
            if (newValues.get(i) != null && !newValues.get(i).trim().isEmpty()) {
                items.get(i).setTitle(newValues.get(i).trim());
            }
        }
    }
}