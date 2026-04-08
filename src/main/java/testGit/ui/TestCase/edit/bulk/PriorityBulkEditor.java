package testGit.ui.TestCase.edit.bulk;

import testGit.pojo.Config;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.persist.PersistenceManager;

import java.util.List;

public class PriorityBulkEditor extends JsonSplitBulkEditor {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Priorities (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getOriginalValue(TestCaseDto tc) {
        return tc.getPriority() != null ? tc.getPriority().name() : "";
    }

    @Override
    protected void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges) {
        String id = "Item-" + (index + 1);
        String escapedTitle = escapeJson(tc.getTitle());
        String priorityStr = tc.getPriority() != null ? tc.getPriority().name() : "";
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
    protected void saveValues(List<TestCaseDto> items, List<String> newValues, Runnable onUpdate) {
        Priority[] newPriorities = new Priority[newValues.size()];

        for (int i = 0; i < newValues.size(); i++) {
            String val = newValues.get(i).trim();
            Priority matched = items.get(i).getPriority();

            for (Priority p : Priority.values()) {
                if (p.name().equalsIgnoreCase(val)) {
                    matched = p;
                    break;
                }
            }
            newPriorities[i] = matched;
        }

        PersistenceManager.getInstance(Config.getProject()).updatePriority(items, newPriorities, onUpdate);
    }
}