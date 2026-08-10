package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

public class TestDataBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public TestDataBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Test Data (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getJsonFieldName() {
        return "testData";
    }

    @Override
    protected String getOriginalValue(final TestCaseDto tc) {
        return tc.getTestData();
    }

    @Override
    protected void setValue(final TestCaseDto tc, final String value) {
        tc.setTestData(value);
    }
}
