package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CopyTestCase extends DumbAwareAction {
    private final @NotNull JBList<TestCaseDto> list;

    public CopyTestCase(final @NotNull JBList<TestCaseDto> list) {
        super("Copy", "Copy test case", AllIcons.Actions.Copy);
        this.list = list;
        registerCustomShortcutSet(KeyboardSet.CopyTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TestCaseDto tc = list.getSelectedValue();
        if (tc == null || e.getProject() == null) return;

        final String text = Arrays.stream(TestEditorAttributes.values())
                .filter(TestEditorAttributes::isCopyable)
                .map(attr -> attr.getName2() + " " + attr.getValueExtractor().apply(tc, e.getProject()))
                .collect(Collectors.joining("\n"));

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}