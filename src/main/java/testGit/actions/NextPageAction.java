package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.IEditorUI;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

public class NextPageAction extends DumbAwareAction {
    private final IEditorUI ui;

    public NextPageAction(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Next Page", "Navigate to the next page", AllIcons.Actions.Forward);
        this.ui = ui;
        registerCustomShortcutSet(KeyboardSet.NextTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (ui.getCurrentPage() < ui.getTotalPageCount()) {
            ui.setCurrentPage(ui.getCurrentPage() + 1);
            ui.refreshView();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        int current = ui.getCurrentPage();
        int total = ui.getTotalPageCount();
        e.getPresentation().setEnabled(current < total);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
