package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;

public class CreateTestCaseBtn extends AbstractIconButton implements ToolbarItem {

    // UC-EDITOR-PANEL-005
    public CreateTestCaseBtn(final @NotNull Runnable onToolBarCreateTestCaseClicked) {
        super("Create test case", AllIcons.General.Add, Declared.shortcutText("Testin.CreateTestCase"));

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}
