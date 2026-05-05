package org.testin.ui.testCase.update.bulk;

import org.testin.pojo.Priority;
import org.testin.pojo.dto.TestCaseDto;

import java.util.List;

public class PriorityBulkSection extends JsonSplitBulkSection {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Priorities (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getOriginalValue(TestCaseDto tc) {
        return tc.getPriority().name();
    }

    @Override
    protected void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
        String id = "Item-" + (index + 1);
        String escapedTitle = escapeJson(tc.getDescription());
        String priorityStr = tc.getPriority().name();
        String escapedPriority = escapeJson(priorityStr);

        String prefix = "  {\n    \"id\": \"" + id + "\",\n    \"title\": \"" + escapedTitle + "\",\n    \"priority\": \"";
        String suffix = "\"\n  }";
        String comma = isLast ? "\n" : ",\n";

        leftSb.append(prefix).append(escapedPriority).append(suffix).append(comma);

        rightSb.append(prefix);
        int startOffset = rightSb.length();
        rightSb.append(escapedPriority);
        int endOffset = rightSb.length();
        rightEditableRanges.add(new int[]{startOffset, endOffset});
        rightSb.append(suffix).append(comma);
    }

    @Override
    protected void applyValues(final List<TestCaseDto> items, final List<String> newValues) {
        for (int i = 0; i < items.size(); i++) {
            String val = newValues.get(i).trim();
            if (!val.isEmpty()) {
                try {
                    items.get(i).setPriority(Priority.valueOf(val.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}