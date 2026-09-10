package org.testin.clipboard;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CutTestCaseNodeAction extends AbstractProjectAction {

    private final @NotNull TestinEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public CutTestCaseNodeAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Cut Node", "Cut selected test case(s) to clipboard", AllIcons.Actions.MenuCut);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.CutTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        List<TestCaseDto> selectedTestCases = list.getSelectedValuesList();

        if (!selectedTestCases.isEmpty()) {
            try {
                Services.getInstance(p, CutState.class).cut(editor, selectedTestCases);

                String json = Services.getInstance(p, Mapper.class).writeValueAsString(selectedTestCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                list.repaint();

                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.CUT, selectedTestCases.size());

            } catch (final Exception ex) {
                Logger.error("Exception: " + ex.getMessage());
            }
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-EDITOR-PANEL-214. Gray with the reason on a node that cannot lose
        // a test case, rather than absent from that editor's menu (#248).
        if (!editor.getParent().isTestCaseContainer()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription("A test run's test cases were chosen when it was created. Cut the test case in its test set.");
            return;
        }

        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
