package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CopyTestCaseAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;

    public CopyTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super("Copy", "Copy test case", AllIcons.Actions.Copy);
        this.p = p;
        this.list = list;
        registerCustomShortcutSet(KeyboardSet.CopyTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TestCaseDto tc = list.getSelectedValue();
        if (tc == null) return;

        final String text = Arrays.stream(TestEditorAttributes.values())
                .filter(TestEditorAttributes::isCopyable)
                .map(attr -> attr.getName2() + " " + attr.getValueExtractor().apply(tc, p))
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