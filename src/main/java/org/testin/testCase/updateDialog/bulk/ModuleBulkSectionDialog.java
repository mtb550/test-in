package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

public class ModuleBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public ModuleBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Modules (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected String getJsonFieldName() {
        return "module";
    }

    @Override
    protected String getOriginalValue(final TestCaseDto tc) {
        return tc.getModule();
    }

    @Override
    protected void setValue(final TestCaseDto tc, final String value) {
        tc.setModule(value);
    }
}
