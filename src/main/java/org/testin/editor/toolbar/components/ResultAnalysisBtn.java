package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.model.TestRunStatus;

/**
 * Writes what the run means: a paragraph per verdict, printed in the reports
 * under the counts.
 * <p>
 * Shown always and enabled only once the run is completed. Disabled rather than
 * hidden, because a button that appears when a run finishes is a button the
 * tester has to notice; one that is there from the start, greyed, says the work
 * exists and when it can be done - and the tooltip says why it cannot yet.
 * <p>
 * Completed and not merely terminal: a closed run is finished with, and writing
 * an analysis into it is describing a run nobody will act on.
 */
public class ResultAnalysisBtn extends AbstractButton implements ToolbarItem {

    private final @NotNull Toolbar callbacks;

    public ResultAnalysisBtn(final @NotNull Toolbar callbacks, final @NotNull Runnable onResultAnalysisClicked) {
        // The platform's own analysis icon. The one this was asked for -
        // ExceptionAnalyzerIcons expui/exceptionAnalyzer - ships with the
        // ExceptionAnalyzer plugin rather than the platform, so naming it would
        // make Testin refuse to load without that plugin installed.
        //
        // Off rather than On: the two differ only in color, and the On variant is
        // green. Green on a toolbar reads as something being switched on, and
        // this is a button that opens a dialog.
        super("Result Analysis", AllIcons.Actions.ProjectWideAnalysisOff);
        this.callbacks = callbacks;

        addActionListener(e -> onResultAnalysisClicked.run());
        updateEnabledState();
    }

    public void updateEnabledState() {
        if (!(callbacks instanceof RunEditor editor)) return;

        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        final boolean completed = status == TestRunStatus.COMPLETED;

        setEnabled(completed);
        setToolTipText(completed
                ? "Result Analysis"
                : "Result Analysis is written once the run is completed — it is " + status.getLabel());
    }
}
