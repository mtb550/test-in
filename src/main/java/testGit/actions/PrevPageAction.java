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

public class PrevPageAction extends DumbAwareAction {
    private final IEditorUI ui;

    public PrevPageAction(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Previous Page", "Navigate to the previous page", AllIcons.Actions.Back);
        this.ui = ui;
        registerCustomShortcutSet(KeyboardSet.PreviousTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (ui.getCurrentPage() > 1) {
            ui.setCurrentPage(ui.getCurrentPage() - 1);
            ui.refreshView();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        int current = ui.getCurrentPage();
        e.getPresentation().setEnabled(current > 1);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}