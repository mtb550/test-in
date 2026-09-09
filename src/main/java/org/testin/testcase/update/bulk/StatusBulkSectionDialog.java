package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.TestDataParser;

import java.util.List;
import java.util.function.Consumer;

/**
 * One status across a selection, edited as text like the priority beside it.
 */
public class StatusBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public StatusBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Statuses";
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "status";
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }

    @Override
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto tc) {
        return tc.getStatus().getLabel();
    }

    @Override
    protected void setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        // Through the parser, like every other reader of this text: it takes the
        // label the tester is looking at rather than the constant name, so "To
        // Be Updated" is read as the status it names. A word it cannot read
        // leaves the case with the status it already had, which is the answer a
        // typo deserves here - the alternative is silently choosing one.
        TestDataParser.testCaseStatus(value, tc.getStatus()).ifPresent(tc::setStatus);
    }
}
