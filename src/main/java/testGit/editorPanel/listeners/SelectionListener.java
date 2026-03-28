package testGit.editorPanel.listeners;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import testGit.editorPanel.BaseEditorUI;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.nio.file.Path;

public class SelectionListener implements ListSelectionListener {
    private final JBList<TestCaseDto> list;
    private final ToolWindow toolWindow = ViewPanel.getToolWindow();
    private final BaseEditorUI ui;
    private final Path path;

    public SelectionListener(final JBList<TestCaseDto> list, final BaseEditorUI ui, final Path path) {
        this.list = list;
        this.ui = ui;
        this.path = path;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            TestCaseDto selected = list.getSelectedValue();
            if (selected != null) {
                if (toolWindow != null && toolWindow.isVisible()) {
                    ViewPanel.show(selected, path);
                }
                if (ui.getStatusBar() != null) {
                    ui.getStatusBar().updateSelectionState(
                            list.getSelectedIndices(),
                            ui.getCurrentPage(),
                            ui.getPageSize(),
                            ui.getTotalItemsCount()
                    );
                }
            }
        }
    }
}