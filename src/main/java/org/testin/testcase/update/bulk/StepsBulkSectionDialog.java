package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StepsBulkSectionDialog extends JsonArraySplitBulkSectionDialog {

    public StepsBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems,
                                  final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Bulk Edit Steps";
    }

    @Override
    protected @NotNull String getArrayFieldName() {
        return "steps";
    }

    @Override
    protected @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items) {
        final List<List<String>> originalSteps = new ArrayList<>();

        for (final TestCaseDto tc : items) {
            originalSteps.add(new ArrayList<>(tc.getSteps()));
        }

        return originalSteps;
    }

    @Override
    protected void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues) {
        for (int i = 0; i < items.size(); i++) {
            final List<String> cleanSteps = new ArrayList<>();

            for (final String step : newValues.get(i)) {
                if (step == null) continue;
                final String cleanStr = step.trim();

                if (!cleanStr.isEmpty()) {
                    cleanSteps.add(cleanStr);
                }
            }

            items.get(i).setSteps(cleanSteps);
        }
    }
}