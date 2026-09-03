package org.testin.editor.statusbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;


/**
 * Page navigation action for the editors; the subclasses only choose direction.
 */
abstract class AbstractPageAction extends DumbAwareAction {
    protected final @NotNull TestinEditor editor;
    private final @NotNull PageStep step;

    protected AbstractPageAction(final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull PageStep step) {
        super(step.getTooltip(), step.getDescription(), step.getIcon());
        this.editor = editor;
        this.step = step;
        registerCustomShortcutSet(step.getShortcut().getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        editor.stepPage(step.deltaFrom(editor.getCurrentPage(), editor.getTotalPageCount()));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(step.isAvailable(editor.getCurrentPage(), editor.getTotalPageCount()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT although update() reads no Swing component: the page state it does
        // read is owned by the EDT - actionPerformed writes currentPage there -
        // so a background read would enable or disable the button from a stale
        // page number (#52).
        return ActionUpdateThread.EDT;
    }
}
