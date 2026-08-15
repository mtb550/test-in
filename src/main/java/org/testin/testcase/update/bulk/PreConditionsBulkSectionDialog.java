package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.function.Consumer;

public class PreConditionsBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public PreConditionsBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems,
                                           final @Nullable Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Pre-Conditions";
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "preCondition";
    }

    @Override
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto tc) {
        return tc.getPreConditions();
    }

    @Override
    protected void setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        tc.setPreConditions(value);
    }
}
