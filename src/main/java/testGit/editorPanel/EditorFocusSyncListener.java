package testGit.editorPanel;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.VirtualFileImpl;
import testGit.pojo.TestCase;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;

public class EditorFocusSyncListener implements FileEditorManagerListener {
    private final JBList<TestCase> list;

    public EditorFocusSyncListener(JBList<TestCase> list) {
        this.list = list;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (event.getNewFile() instanceof VirtualFileImpl) {
            System.out.println(this.getClass() + " slection changed");
            ToolWindow viewPanel = ViewPanel.getToolWindow();

            if (viewPanel != null && viewPanel.isVisible()) {
                TestCase selected = list.getSelectedValue();
                if (selected != null) {
                    SwingUtilities.invokeLater(() -> ViewPanel.show(selected));
                }
            }
        }
    }

}
