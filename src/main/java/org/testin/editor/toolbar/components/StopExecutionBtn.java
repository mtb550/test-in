package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class StopExecutionBtn extends AbstractIconButton implements ToolbarItem {

    public StopExecutionBtn(final @NotNull Runnable onStopExecutionClicked) {
        // https://intellij-icons.jetbrains.design/
        super("Stop Execution", AllIcons.Debugger.ThreadFrozen);

        addActionListener(e -> onStopExecutionClicked.run());
    }
}
