package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Set;

/**
 * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-033.
 * <p>
 * The steps of a test case, one box each, added with CTRL+S.
 * <p>
 * The rows, the key and what is saved are {@link AbstractMultiValueSection}'s -
 * groups work the same way since they became words rather than constants
 * (#296). What is this section's own is the numbering: a step's place is part of
 * what it says, so the placeholder counts.
 */
public class StepsSection extends AbstractMultiValueSection {

    public StepsSection(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull CreateTestCaseFields field() {
        return CreateTestCaseFields.STEPS;
    }

    @Override
    protected @NotNull Set<String> completions(final @NotNull TestCaseValues cache) {
        return cache.getSteps();
    }

    @Override
    protected @NotNull List<String> valuesOf(final @NotNull TestCaseDto dto) {
        return dto.getSteps();
    }

    @Override
    protected void write(final @NotNull TestCaseDto dto, final @NotNull List<String> values) {
        dto.setSteps(values);
    }

    @Override
    protected @NotNull Shortcuts addKey() {
        return Shortcuts.CreateTestCaseAddStep;
    }

    /**
     * "Step 1", "Step 2". The order is what a step means, so the row says which
     * one it is before anything is typed in it.
     */
    @Override
    protected @NotNull String placeholderFor(final int index) {
        return CreateTestCaseFields.STEPS.getPlaceholder() + (index + 1);
    }
}
