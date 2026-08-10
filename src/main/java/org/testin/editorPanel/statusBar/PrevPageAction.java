package org.testin.editorPanel.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

public class PrevPageAction extends AbstractPageAction {

    public PrevPageAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(editor, list, "Previous Page", "Navigate to the previous page", AllIcons.Actions.Back, KeyboardSet.PreviousTestCase, -1);
    }
}
