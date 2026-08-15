package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.util.text.HtmlChunk;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

public class CreateTestCaseBtn extends AbstractButton implements ToolbarItem {

    public CreateTestCaseBtn(final @NotNull Runnable onToolBarCreateTestCaseClicked) {
        super(null, AllIcons.General.Add);

        new HelpTooltip()
                .setDescription(HtmlChunk.text("Create test case"))
                .setShortcut(Shortcuts.CreateItem.getShortcut())
                .installOn(this);

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}