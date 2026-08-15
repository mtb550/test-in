package org.testin.editor.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;

public class NextPageAction extends AbstractPageAction {

    public NextPageAction(final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(editor, list, "Next Page", "Navigate to the next page", AllIcons.Actions.Forward, Shortcuts.NextTestCase, 1);
    }
}
