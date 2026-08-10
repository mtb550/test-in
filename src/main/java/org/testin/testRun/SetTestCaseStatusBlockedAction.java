package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.enums.TestStatus;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.util.Tools;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

public class SetTestCaseStatusBlockedAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_B, 0);
    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetTestCaseStatusBlockedAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Blocked", "Set test case status to Blocked", AllIcons.Actions.Pause);
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, TestStatus.BLOCKED);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
