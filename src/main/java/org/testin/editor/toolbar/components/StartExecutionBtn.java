package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.model.TestRunStatus;

public class StartExecutionBtn extends AbstractButton implements ToolbarItem {

    private final @NotNull Toolbar callbacks;

    public StartExecutionBtn(final @NotNull Toolbar callbacks, final @NotNull Runnable onStartExecutionClicked) {
        super(Toolbar.START_MANUAL_EXECUTION, Toolbar.START_MANUAL_EXECUTION_ICON);
        this.callbacks = callbacks;

        addActionListener(e -> onStartExecutionClicked.run());
    }

    private static @NotNull String tooltipFor(final @NotNull RunEditor editor) {
        if (editor.isExecuting()) return "Execution in progress";

        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        return status.isTerminal()
                ? "Execution disabled — run status is " + status.getLabel()
                : Toolbar.START_MANUAL_EXECUTION;
    }

    public void updateEnabledState() {
        if (!(callbacks instanceof RunEditor editor)) return;

        setEnabled(editor.canStartExecution());
        setToolTipText(tooltipFor(editor));
    }
}