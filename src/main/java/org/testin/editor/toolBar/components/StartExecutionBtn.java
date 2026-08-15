package org.testin.editor.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.runEditor.RunEditor;
import org.testin.editor.toolBar.Toolbar;
import org.testin.enums.TestRunStatus;

public class StartExecutionBtn extends AbstractButton implements ToolbarItem {

    private final @NotNull Toolbar callbacks;

    public StartExecutionBtn(final @NotNull Toolbar callbacks, final @NotNull Runnable onStartExecutionClicked) {
        super("Start Execution", AllIcons.Actions.Execute);
        this.callbacks = callbacks;

        addActionListener(e -> onStartExecutionClicked.run());
    }


    public void updateEnabledState() {
        if (!(callbacks instanceof RunEditor editor)) return;

        setEnabled(editor.canStartExecution());
        setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Actions.Execute));
        setToolTipText(tooltipFor(editor));
    }

    private static @NotNull String tooltipFor(final @NotNull RunEditor editor) {
        if (editor.isExecuting()) return "Execution in progress";

        final TestRunStatus status = editor.getParent().getMarker().getStatus();
        return status.isTerminal()
                ? "Execution disabled — run status is " + status.getLabel()
                : "Start Execution";
    }
}