package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.Toolbar;

public class StopExecutionBtn extends AbstractIconButton implements ToolbarItem {

    public StopExecutionBtn(final @NotNull Runnable onStopExecutionClicked) {
        // https://intellij-icons.jetbrains.design/
        super(Toolbar.STOP_EXECUTION, Toolbar.STOP_EXECUTION_ICON);

        addActionListener(e -> onStopExecutionClicked.run());
    }
}
