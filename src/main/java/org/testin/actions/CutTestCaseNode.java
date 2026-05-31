package org.testin.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.EditorCM;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testCaseEditor.TestEditorUI;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.List;

public class CutTestCaseNode extends DumbAwareAction {
    private final IEditorUI editorUI;
    private final JBList<TestCaseDto> list;
    private final EditorCM editorCM;

    public CutTestCaseNode(final IEditorUI editorUI, final JBList<TestCaseDto> list, final EditorCM editorCM) {
        super("Cut Node", "Cut selected test case(s) to clipboard", AllIcons.Actions.MenuCut);
        this.editorUI = editorUI;
        this.list = list;
        this.editorCM = editorCM;
        this.registerCustomShortcutSet(KeyboardSet.CutTestCaseNode.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        System.out.println("[DEBUG] CutTestCaseNode: actionPerformed triggered.");

        List<TestCaseDto> selectedTestCases = list.getSelectedValuesList();
        System.out.println("[DEBUG] CutTestCaseNode: Selected items count = " + selectedTestCases.size());

        if (!selectedTestCases.isEmpty()) {
            try {
                editorCM.setCutAction(true);

                System.out.println("[DEBUG] CutTestCaseNode: Attempting to serialize with Jackson...");
                ObjectMapper mapper = Config.getMapper();
                String json = mapper.writeValueAsString(selectedTestCases);

                CopyPasteManager.getInstance().setContents(new StringSelection(json));
                System.out.println("[DEBUG] CutTestCaseNode: Data successfully pushed to System Clipboard.");

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!(editorUI instanceof TestEditorUI ui)) {
                        System.out.println("[DEBUG] CutTestCaseNode: editorUI is not an instance of TestEditorUI. Aborting removal.");
                        return;
                    }

                    for (TestCaseDto tc : selectedTestCases) {
                        ui.getAllTestCases().remove(tc);

                        File file = new File(ui.getVf().getTestSet().getPath().toFile(), tc.getId() + ".json");
                        if (file.exists() && file.delete()) {
                            System.out.println("[DEBUG] CutTestCaseNode: Deleted physical file for ID: " + tc.getId());
                        } else {
                            System.out.println("[WARNING] CutTestCaseNode: Failed to delete physical file for ID: " + tc.getId());
                        }
                    }

                    System.out.println("[DEBUG] CutTestCaseNode: Triggering sequence update and save...");
                    ui.sortAndIdentifyUnsorted();
                    ui.updateSequenceAndSaveAll();

                    System.out.println("Successfully cut and removed " + selectedTestCases.size() + " test cases.");
                });

            } catch (Exception ex) {
                System.err.println("[ERROR] CutTestCaseNode: Failed to cut: " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        } else {
            System.out.println("[DEBUG] CutTestCaseNode: Aborted. No items were selected in the JBList.");
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // todo, to be implemented. left empty for testing functionality.
    }
}