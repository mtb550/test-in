package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.testin.editorPanel.testRunEditor.RunEditorUI;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.TestRunStatus;

public class StartExecutionBtn extends AbstractButton implements IToolbarItem {

    private final IToolBar callbacks;

    public StartExecutionBtn(final IToolBar callbacks, final Runnable onStartExecutionClicked) {
        super("Start Execution", AllIcons.Nodes.Services);
        this.callbacks = callbacks;

        addActionListener(e -> onStartExecutionClicked.run());
    }


    public void updateEnabledState() {
        if (callbacks instanceof RunEditorUI runUi) {
            TestRunStatus status = runUi.getVf().getTestRun().getMarker().getStatus();
            if (status == TestRunStatus.CLOSED || status == TestRunStatus.COMPLETED) {
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