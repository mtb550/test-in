package org.testin.automate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class AutomateTestCaseAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK);
    private final @NotNull Project p;
    private final JBList<TestCaseDto> list;

    public AutomateTestCaseAction(final @NotNull Project p, final JBList<TestCaseDto> list) {
        super("Automate Test Case ", "", AllIcons.Actions.IntentionBulb);
        this.p = p;
        this.list = list;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();

        /// TODO: to be implemented by integrating to pi agent automatic, next release
        Logger.info(tc.getDescription());
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
