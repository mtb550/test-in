package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Priority;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;

public class PriorityBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public PriorityBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Priorities (Enter to Save | Tab/Arrows to Navigate)";
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
        return tc.getPriority().name();
    }

    @Override
    protected void setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        try {
            tc.setPriority(Priority.valueOf(value.toUpperCase()));
        } catch (final IllegalArgumentException ex) {
            Logger.error(ex.getMessage());
        }
    }
}
