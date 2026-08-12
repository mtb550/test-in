package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.enums.TestRunStatus;

public class StartExecutionBtn extends AbstractButton implements IToolbarItem {

    private final @NotNull IToolBar callbacks;

    public StartExecutionBtn(final @NotNull IToolBar callbacks, final @NotNull Runnable onStartExecutionClicked) {
        super("Start Execution", AllIcons.Nodes.Services);
        this.callbacks = callbacks;

        addActionListener(e -> onStartExecutionClicked.run());
    }


    public void updateEnabledState() {
        if (callbacks instanceof RunEditor editor) {
            final TestRunStatus status = editor.getParent().getMarker().getStatus();

            if (status.isTerminal()) {
                setEnabled(false);
                setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Nodes.Services));
                setToolTipText("Execution disabled — run status is " + status.getLabel());

            } else {
                setEnabled(true);
                setIcon(AllIcons.Nodes.Services);
                setToolTipText("Start Execution");
            }
        }
    }
}