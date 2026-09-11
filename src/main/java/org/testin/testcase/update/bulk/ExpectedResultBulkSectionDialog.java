package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;


public class ExpectedResultBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public ExpectedResultBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull TestEditorAttributes attribute() {
        return TestEditorAttributes.EXPECTED_RESULT;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.expected.result");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "expectedResult";
    }


}
