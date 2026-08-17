package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class StopExecutionBtn extends AbstractButton implements ToolbarItem {

    public StopExecutionBtn(final @NotNull Runnable onStopExecutionClicked) {
        // https://intellij-icons.jetbrains.design/
        super("Stop Execution", AllIcons.Debugger.ThreadFrozen);

        addActionListener(e -> onStopExecutionClicked.run());
    }
}
