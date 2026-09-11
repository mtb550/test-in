package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;


public class TestDataBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public TestDataBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull TestEditorAttributes attribute() {
        return TestEditorAttributes.TEST_DATA;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.test.data");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "testData";
    }


}
