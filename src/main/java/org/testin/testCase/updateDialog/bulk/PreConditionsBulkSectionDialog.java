package org.testin.testCase.updateDialog.bulk;

import org.testin.mappers.dto.TestCaseDto;

import java.util.List;

public class PreConditionsBulkSectionDialog extends JsonSplitBulkSectionDialog {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Pre-Conditions (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getOriginalValue(final TestCaseDto tc) {
        return tc.getPreConditions();
    }

    @Override
    protected void appendJsonItem(final TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
        String id = escapeJson(tc.getId().toString());
        String escapedDescription = escapeJson(tc.getDescription());
        // todo, add expected result to be shown once update bulk pre-conditions

        String rawPreCondition = tc.getPreConditions();
        String escapedPreCondition = escapeJson(rawPreCondition);

        String prefix = "  {\n    \"id\": \"" + id + "\",\n    \"description\": \"" + escapedDescription + "\",\n    \"preCondition\": \"";
        String suffix = "\"\n  }";
        String comma = isLast ? "\n" : ",\n";

        leftSb.append(prefix).append(escapedPreCondition).append(suffix).append(comma);

        rightSb.append(prefix);
        int startOffset = rightSb.length();
        rightSb.append(escapedPreCondition);
        int endOffset = rightSb.length();
        rightEditableRanges.add(new int[]{startOffset, endOffset});
        rightSb.append(suffix).append(comma);
    }

    @Override
    protected void applyValues(final List<TestCaseDto> items, final List<String> newValues) {
        for (int i = 0; i < items.size(); i++) {
            if (newValues.get(i) != null) {
                items.get(i).setPreConditions(newValues.get(i).trim());
            }
        }
    }
}