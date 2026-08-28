package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

public class CreateTestCaseBtn extends AbstractIconButton implements ToolbarItem {

    public CreateTestCaseBtn(final @NotNull Runnable onToolBarCreateTestCaseClicked) {
        super("Create test case", AllIcons.General.Add, Shortcuts.CreateItem);

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}
