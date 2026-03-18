package testGit.editorPanel;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;

public class EditorFocusSyncListener implements FileEditorManagerListener {
    private final JBList<TestCaseDto> list;

    public EditorFocusSyncListener(JBList<TestCaseDto> list) {
        this.list = list;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {

        if (event.getNewFile() instanceof UnifiedVirtualFile) {
            System.out.println(this.getClass() + " slection changed");
            ToolWindow viewPanel = ViewPanel.getToolWindow();

            if (viewPanel != null && viewPanel.isVisible()) {
                TestCaseDto selected = list.getSelectedValue();
                if (selected != null) {
                    SwingUtilities.invokeLater(() -> ViewPanel.show(selected));
                }
            }
        }
    }

}
