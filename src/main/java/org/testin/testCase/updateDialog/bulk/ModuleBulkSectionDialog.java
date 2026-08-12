package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

public class ModuleBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public ModuleBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Modules (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "module";
    }

    @Override
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto tc) {
        return tc.getModule();
    }

    @Override
    protected void setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        tc.setModule(value);
    }
}
