package org.testin.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.EditorCM;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopyTestCaseNode extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public CopyTestCaseNode(final JBList<TestCaseDto> list) {
        super("Copy Node", "Copy selected test case(s) to clipboard", AllIcons.Actions.Copy);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.CopyTestCaseNode.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCaseDto> selectedTestCases = list.getSelectedValuesList();

        if (!selectedTestCases.isEmpty()) {
            try {
                EditorCM.clearCutState();

                ObjectMapper mapper = Config.getMapper();
                String json = mapper.writeValueAsString(selectedTestCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // todo, to be implemented. left empty for testing functionality.
    }
}