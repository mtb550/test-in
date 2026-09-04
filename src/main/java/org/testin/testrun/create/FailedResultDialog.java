package org.testin.testrun.create;

import org.testin.model.RunEditorAttributes;
import org.testin.model.TestEditorAttributes;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.framework.*;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Collects the failure details when a test case is set to Failed: the actual
 * result (with the test case's description and expected result as muted
 * context), bug severity and bug priority — all framework components. Saves
 * on Enter, cancels on Escape; nothing is applied unless saved.
 */
public class FailedResultDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull TestRunItems runItem;
    private final @NotNull Runnable onSave;
    private final @NotNull FailureFields fields;

    public FailedResultDialog(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull Runnable onSave) {
        super(p);
        this.runItem = runItem;
        this.onSave = onSave;

        // The case is wired lazily by the run editor; a run item whose test case
        // no longer exists in the test set never gets one.
        final @NotNull Optional<TestCaseDto> tc = runItem.testCase();

        fields = new FailureFields(runItem);

        title = "Failed Test Case Details";

        // The dialog still opens without the test case: the tester came here to
        // read and edit the recorded result, which lives on the run item. Only
        // the two rows describing the case are affected, and the Expected row
        // is left blank rather than filled with an apology - DialogDetails drops
        // blank rows, so it simply does not appear.
        final @NotNull String description = tc.map(TestCaseDto::getDescription).orElse("No longer in the test set");
        final @NotNull String expectedResult = tc.map(TestCaseDto::getExpectedResult).orElse("");

        final @NotNull List<ComponentDialogBase<?>> all = new ArrayList<>();
        all.add(ComponentDialogBase.details()
                .row(TestEditorAttributes.DESCRIPTION.getName(), description)
                .row("Expected", expectedResult)
                .build());
        all.addAll(fields.components());

        components = all;

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Save", this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    @Override
    protected void submit() {
        // Applied only on save - Escape must never commit the edit.
        fields.applyTo(runItem);

        onSave.run();
        closeOk();
    }
}
