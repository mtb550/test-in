package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.util.text.HtmlChunk;
import org.testin.util.KeyboardSet;

public class CreateTestCaseBtn extends AbstractButton implements IToolbarItem {

    public CreateTestCaseBtn(final Runnable onToolBarCreateTestCaseClicked) {
        super(null, AllIcons.General.Add);

        new HelpTooltip()
                // todo, deprecated
                .setDescription(HtmlChunk.text("Create test case"))
                .setShortcut(KeyboardSet.CreateTestCase.getShortcut())
                .installOn(this);

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}