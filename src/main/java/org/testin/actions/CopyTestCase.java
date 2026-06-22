package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public CopyTestCase(final JBList<TestCaseDto> list) {
        super("Copy", "Copy test case", AllIcons.Actions.Copy);
        this.list = list;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();
        String text = "Description: " + tc.getDescription() + "\nSteps: " + tc.getSteps() + "\nExpected result: " + tc.getExpectedResult();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
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
