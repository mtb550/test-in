package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;

import static org.testin.importexport.imports.ImportSetter.always;

public class ModuleBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public ModuleBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.module");
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
    protected boolean setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        return always(() -> tc.setModule(value));
    }
}
