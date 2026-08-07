package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

public class StepsBulkSectionDialog extends JsonArraySplitBulkSectionDialog {

    public StepsBulkSectionDialog(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Steps";
    }

    @Override
    protected String getArrayFieldName() {
        return "steps";
    }

    @Override
    protected List<List<String>> extractOriginalValues(final List<TestCaseDto> items) {
        List<List<String>> originalSteps = new ArrayList<>();

        for (TestCaseDto tc : items) {
            originalSteps.add(new ArrayList<>(tc.getSteps()));
        }

        return originalSteps;
    }

    @Override
    protected void applyValues(final List<TestCaseDto> items, final List<List<String>> newValues) {
        for (int i = 0; i < items.size(); i++) {
            List<String> cleanSteps = new ArrayList<>();

            for (String step : newValues.get(i)) {
                if (step == null) continue;
                String cleanStr = step.trim();

                if (!cleanStr.isEmpty()) {
                    cleanSteps.add(cleanStr);
                }
            }

            items.get(i).setSteps(cleanSteps);
        }
    }
}