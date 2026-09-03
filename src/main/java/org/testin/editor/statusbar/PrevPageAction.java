package org.testin.editor.statusbar;

import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;

public class PrevPageAction extends AbstractPageAction {

    public PrevPageAction(final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(editor, list, PageStep.PREVIOUS);
    }
}
