package org.testin.testrun.create;

import org.jetbrains.annotations.NotNull;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RadioSelection;
import org.testin.ui.framework.TextArea;
import org.testin.ui.framework.TextInput;

import java.util.List;

/**
 * The four things a tester writes down about a failure: what actually happened,
 * how bad it is, how urgent it is, and the error itself.
 * <p>
 * <b>One declaration, two places to fill it in.</b> {@link FailedResultDialog}
 * asks for them in the run editor; light mode asks for them inside its own
 * window, because that dialog is modal and owned by the IDE frame, so opening
 * it would raise IntelliJ and put the tester back exactly where light mode
 * exists to keep them out of (#13). A failure recorded in one place and a
 * failure recorded in the other have to be the same record rather than two
 * shapes of one - so the fields, their defaults, their wording and the order
 * they are written back in are declared here and nowhere else.
 * <p>
 * Built against one run row and holding it: these are that row's values, and a
 * second row's would need a second set of fields.
 */
public final class FailureFields {

    private final @NotNull ComponentDialogBase<TextInput> actualResult;
    private final @NotNull ComponentDialogBase<RadioSelection<BugSeverity>> severity;
    private final @NotNull ComponentDialogBase<RadioSelection<BugPriority>> priority;
    private final @NotNull ComponentDialogBase<TextArea> errorCapture;

    public FailureFields(final @NotNull TestRunItems runItem) {
        actualResult = ComponentDialogBase.textField()
                .placeholder("set actual result..")
                .value(runItem.getActualResult())
                .build();

        severity = ComponentDialogBase.<BugSeverity>radios(RunEditorAttributes.BUG_SEVERITY.getName())
                .options(BugSeverity.CHOICES, BugSeverity::getLabel)
                .select(BugSeverity.orDefault(runItem.getBugSeverity()))
                .build();

        priority = ComponentDialogBase.<BugPriority>radios(RunEditorAttributes.BUG_PRIORITY.getName())
                .options(BugPriority.CHOICES, BugPriority::getLabel)
                .select(BugPriority.orDefault(runItem.getBugPriority()))
                .build();

        errorCapture = ComponentDialogBase.textArea()
                .placeholder("paste error or exception or screenshot..")
                .value(runItem.getStacktrace())
                .rows(5)
                .build();
    }

    /**
     * The four, in the order a tester fills them in: what happened, then how
     * much it matters, then the evidence.
     */
    public @NotNull List<? extends ComponentDialogBase<?>> components() {
        return List.of(actualResult, severity, priority, errorCapture);
    }

    /**
     * Writes what was typed onto the run row.
     * <p>
     * Only ever called by a save. Escape must never commit an edit, so nothing
     * here happens as the tester types.
     */
    public void applyTo(final @NotNull TestRunItems runItem) {
        runItem.setActualResult(actualResult.getComponent().getText().trim());
        runItem.setBugSeverity(severity.getComponent().getSelected());
        runItem.setBugPriority(priority.getComponent().getSelected());
        runItem.setStacktrace(errorCapture.getComponent().getText().trim());
    }

    /**
     * Where the tester starts typing.
     */
    public @NotNull TextInput firstField() {
        return actualResult.getComponent();
    }
}
