package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class CopyTestCaseNodeAction extends AbstractProjectAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
    private final @NotNull JBList<TestCaseDto> list;

    public CopyTestCaseNodeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Copy Node", "Copy selected test case(s) to clipboard", AllIcons.Actions.Copy);
        this.list = list;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final List<TestCaseDto> tcs = list.getSelectedValuesList();

        if (!tcs.isEmpty()) {
            try {
                TestEditorContextMenu.clearCutState();

                String json = Services.getInstance(p, Mapper.class).writeValueAsString(tcs);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                Services.getInstance(p, Notifier.class).softShowCounted(p, "Test case", "copied", tcs.size());

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
        return ActionUpdateThread.EDT;
    }
}
