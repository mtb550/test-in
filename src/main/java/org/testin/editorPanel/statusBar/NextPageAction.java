package org.testin.editorPanel.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

public class NextPageAction extends AbstractPageAction {

    public NextPageAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(editor, list, "Next Page", "Navigate to the next page", AllIcons.Actions.Forward, KeyboardSet.NextTestCase, 1);
    }
}
