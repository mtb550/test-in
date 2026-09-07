package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.model.TestRunStatus;

public class StartExecutionBtn extends AbstractIconButton implements ToolbarItem {

    private final @NotNull RunEditor editor;

    public StartExecutionBtn(final @NotNull RunEditor editor, final @NotNull Runnable onStartExecutionClicked) {
        super(Toolbar.START_MANUAL_EXECUTION, Toolbar.START_MANUAL_EXECUTION_ICON);
        this.editor = editor;

        addActionListener(e -> onStartExecutionClicked.run());
    }

    /**
     * UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132.
     * <p>
     * Why the button is gray, in the order the reasons matter: a walk already
     * going, then a run that records nothing more, then a walk with nowhere to
     * land. The last one covers a test run holding no test cases, a filter
     * matching nothing, and a list whose test cases have all been judged - all
     * three of which left the button live and startable (#215).
     */
    private static @NotNull String tooltipFor(final @NotNull RunEditor editor) {
        if (editor.isExecuting()) return "Execution in progress";

        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        if (status.isTerminal()) return "Execution disabled — run status is " + status.getLabel();

        return editor.hasSomethingToWalk()
                ? Toolbar.START_MANUAL_EXECUTION
                : "Nothing to execute — no test case is waiting for a verdict";
    }

    public void updateEnabledState() {
        setEnabled(editor.canStartManualExecution());
        setToolTipText(tooltipFor(editor));
    }
}