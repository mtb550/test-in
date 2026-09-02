package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.TestDataParser;

import java.util.List;
import java.util.function.Consumer;

public class PriorityBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public PriorityBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Priorities";
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "priority";
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }

    @Override
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto tc) {
        return tc.getPriority().getLabel();
    }

    @Override
    protected void setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        // Through the parser, like every other reader of this text: it takes the
        // label the tester is looking at, and valueOf took the constant name -
        // so editing forty cases to P1 set forty of them to P3.
        tc.setPriority(TestDataParser.priority(value));
    }
}
