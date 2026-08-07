package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.Mapper;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopyTestCaseNodeAction extends DumbAwareAction {
    private final @NotNull JBList<TestCaseDto> list;

    public CopyTestCaseNodeAction(final @NotNull JBList<TestCaseDto> list) {
        super("Copy Node", "Copy selected test case(s) to clipboard", AllIcons.Actions.Copy);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.CopyTestCaseNode.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final List<TestCaseDto> tcs = list.getSelectedValuesList();

        if (!tcs.isEmpty()) {
            try {
                TestEditorContextMenu.clearCutState();

                String json = Services.getInstance(e.getProject(), Mapper.class).writeValueAsString(tcs);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

            } catch (final Exception ex) {
                Logger.error("Exception: " + ex.getMessage());
            }
        }
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