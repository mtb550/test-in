package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

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
    protected @NotNull TestEditorAttributes attribute() {
        return TestEditorAttributes.STATUS;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.status");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "status";
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }


}
