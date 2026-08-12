package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

public class DescriptionBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public DescriptionBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Descriptions (Enter to Save | Tab/Arrows to Navigate)";
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "description";
    }

    @Override
    protected boolean showsDescriptionContext() {
        return false;
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }

    @Override
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto tc) {
        return tc.getDescription();
    }

    @Override
    protected void setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        tc.setDescription(value);
    }
}
