package org.testin.editor.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;

public class PrevPageAction extends AbstractPageAction {

    public PrevPageAction(final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(editor, list, "Previous Page", "Navigate to the previous page", AllIcons.Actions.Back, Shortcuts.PreviousTestCase, -1);
    }
}
