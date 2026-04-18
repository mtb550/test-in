package testGit.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import java.awt.datatransfer.StringSelection;

public class CopyTestCaseDescription extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public CopyTestCaseDescription(final JBList<TestCaseDto> list) {
        super("Copy Test Case Title");
        this.list = list;
        registerCustomShortcutSet(KeyboardSet.CopyTestCaseTitle.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TestCaseDto selected = list.getSelectedValue();
        if (selected != null) {
            CopyPasteManager.getInstance().setContents(new StringSelection(selected.getDescription()));
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(list.getSelectedValue() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}