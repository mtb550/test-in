package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.Mapper;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CutTestCaseNodeAction extends DumbAwareAction {
    private final IEditor editor;
    private final JBList<TestCaseDto> list;

    public CutTestCaseNodeAction(final IEditor editor, final JBList<TestCaseDto> list) {
        super("Cut Node", "Cut selected test case(s) to clipboard", AllIcons.Actions.MenuCut);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.CutTestCaseNode.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        Logger.debug("[DEBUG] CutTestCaseNode: actionPerformed triggered.");

        List<TestCaseDto> selectedTestCases = list.getSelectedValuesList();
        Logger.info("[DEBUG] CutTestCaseNode: Selected items count = " + selectedTestCases.size());

        if (!selectedTestCases.isEmpty()) {
            try {
                TestEditorContextMenu.setGlobalCutAction(true);

                TestEditorContextMenu.getGlobalPendingCutIds().clear();
                selectedTestCases.forEach(tc -> TestEditorContextMenu.getGlobalPendingCutIds().add(tc.getId()));
                TestEditorContextMenu.setGlobalSourceEditorUI(editor);

                String json = Services.getInstance(e.getProject(), Mapper.class).writeValueAsString(selectedTestCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                list.repaint();

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