package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

public class ExpectedResultBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public ExpectedResultBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Expected Results (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getJsonFieldName() {
        return "expectedResult";
    }

    @Override
    protected String getOriginalValue(final TestCaseDto tc) {
        return tc.getExpectedResult();
    }

    @Override
    protected void setValue(final TestCaseDto tc, final String value) {
        tc.setExpectedResult(value);
    }
}
