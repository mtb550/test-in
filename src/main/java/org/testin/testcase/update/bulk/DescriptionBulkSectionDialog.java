package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;

import static org.testin.importexport.imports.ImportSetter.always;

public class DescriptionBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public DescriptionBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.description");
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
    protected boolean setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        return always(() -> tc.setDescription(value));
    }
}
