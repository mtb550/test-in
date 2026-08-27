package org.testin.testrun.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.framework.*;
import org.testin.util.Shortcuts;

import java.util.Optional;
import java.util.List;

/**
 * Collects the failure details when a test case is set to Failed: the actual
 * result (with the test case's description and expected result as muted
 * context), bug severity and bug priority — all framework components. Saves
 * on Enter, cancels on Escape; nothing is applied unless saved.
 */
public class FailedResultDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull TestRunItems runItem;
    private final @NotNull Runnable onSave;
    private final @NotNull TextInput actualResult;
    private final @NotNull RadioSelection<BugSeverity> severity;
    private final @NotNull RadioSelection<BugPriority> priority;
    private final @NotNull TextArea errorCapture;

    public FailedResultDialog(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull Runnable onSave) {
        super(p);
        this.runItem = runItem;
        this.onSave = onSave;

        // The case is wired lazily by the run editor; a run item whose test case
        // no longer exists in the test set never gets one.
        final @NotNull Optional<TestCaseDto> tc = runItem.testCase();

        final @NotNull ComponentDialogBase<TextInput> actualResultField = ComponentDialogBase.textField()
                .placeholder("set actual result..")
                .value(runItem.getActualResult())
                .build();
        actualResult = actualResultField.getComponent();

        final @NotNull ComponentDialogBase<RadioSelection<BugSeverity>> severityRadios = ComponentDialogBase.<BugSeverity>radios("Severity")
                .option(BugSeverity.BLOCKER.getName(), BugSeverity.BLOCKER)
                .option(BugSeverity.MAJOR.getName(), BugSeverity.MAJOR)
                .option(BugSeverity.MINOR.getName(), BugSeverity.MINOR)
                .option(BugSeverity.ENHANCEMENT.getName(), BugSeverity.ENHANCEMENT)
                .select(severityOf(runItem))
                .build();
        severity = severityRadios.getComponent();

        final @NotNull ComponentDialogBase<RadioSelection<BugPriority>> priorityRadios = ComponentDialogBase.<BugPriority>radios("Priority")
                .option(BugPriority.HIGH.getName(), BugPriority.HIGH)
                .option(BugPriority.MEDIUM.getName(), BugPriority.MEDIUM)
                .option(BugPriority.LOW.getName(), BugPriority.LOW)
                .select(priorityOf(runItem))
                .build();
        priority = priorityRadios.getComponent();

        final @NotNull ComponentDialogBase<TextArea> errorCaptureArea = ComponentDialogBase.textArea()
                .placeholder("paste error or exception or screenshot..")
                .value(runItem.getStacktrace())
                .rows(5)
                .build();
        errorCapture = errorCaptureArea.getComponent();

        title = "Failed Test Case Details";

        // The dialog still opens without the test case: the tester came here to
        // read and edit the recorded result, which lives on the run item. Only
        // the two rows describing the case are affected, and the Expected row
        // is left blank rather than filled with an apology - DialogDetails drops
        // blank rows, so it simply does not appear.
        final @NotNull String description = tc.map(TestCaseDto::getDescription).orElse("No longer in the test set");
        final @NotNull String expectedResult = tc.map(TestCaseDto::getExpectedResult).orElse("");

        components = List.of(
                ComponentDialogBase.details()
                        .row("Description", description)
                        .row("Expected", expectedResult)
                        .build(),
                actualResultField,
                severityRadios,
                priorityRadios,
                errorCaptureArea);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Save", this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    /**
     * EMPTY is a persistence default, not a choice — Enhancement by default.
     */
    private static @NotNull BugSeverity severityOf(final @NotNull TestRunItems runItem) {
        final @NotNull BugSeverity stored = runItem.getBugSeverity();
        return stored == BugSeverity.EMPTY ? BugSeverity.ENHANCEMENT : stored;
    }

    /**
     * EMPTY is a persistence default, not a choice — Low by default.
     */
    private static @NotNull BugPriority priorityOf(final @NotNull TestRunItems runItem) {
        final @NotNull BugPriority stored = runItem.getBugPriority();
        return stored == BugPriority.EMPTY ? BugPriority.LOW : stored;
    }

    @Override
    protected void submit() {
        // Applied only on save - Escape must never commit the edit.
        runItem.setActualResult(actualResult.getText().trim());
        runItem.setBugSeverity(severity.getSelected());
        runItem.setBugPriority(priority.getSelected());
        runItem.setStacktrace(errorCapture.getText().trim());

        onSave.run();
        closeOk();
    }
}
