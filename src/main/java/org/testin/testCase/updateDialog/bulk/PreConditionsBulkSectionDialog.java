package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

public class PreConditionsBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public PreConditionsBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Pre-Conditions (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getJsonFieldName() {
        return "preCondition";
    }

    @Override
    protected String getOriginalValue(final TestCaseDto tc) {
        return tc.getPreConditions();
    }

    @Override
    protected void setValue(final TestCaseDto tc, final String value) {
        tc.setPreConditions(value);
    }
}
