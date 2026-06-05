package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.EditorCM;
import org.testin.editorPanel.IEditorUI;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CutTestCaseNode extends DumbAwareAction {
    private final IEditorUI editorUI;
    private final JBList<TestCaseDto> list;

    public CutTestCaseNode(final IEditorUI editorUI, final JBList<TestCaseDto> list) {
        super("Cut Node", "Cut selected test case(s) to clipboard", AllIcons.Actions.MenuCut);
        this.editorUI = editorUI;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.CutTestCaseNode.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        Log.debug("[DEBUG] CutTestCaseNode: actionPerformed triggered.");

        List<TestCaseDto> selectedTestCases = list.getSelectedValuesList();
        Log.info("[DEBUG] CutTestCaseNode: Selected items count = " + selectedTestCases.size());

        if (!selectedTestCases.isEmpty()) {
            try {
                EditorCM.setGlobalCutAction(true);

                EditorCM.getGlobalPendingCutIds().clear();
                selectedTestCases.forEach(tc -> EditorCM.getGlobalPendingCutIds().add(tc.getId()));
                EditorCM.setGlobalSourceEditorUI(editorUI);

                String json = Services.getInstance(project, Mapper.class).writeValueAsString(selectedTestCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                list.repaint();

            } catch (Exception ex) {
                Log.error("Exception: " + ex.getMessage());
            }
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // todo, to be implemented. left empty for testing functionality.
    }
}