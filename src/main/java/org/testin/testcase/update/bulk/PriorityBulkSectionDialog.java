package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;


public class PriorityBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public PriorityBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull TestEditorAttributes attribute() {
        return TestEditorAttributes.PRIORITY;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.priority");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "priority";
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }


}
