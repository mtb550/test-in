package testGit.editorPanel.listeners;


import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class SelectionListener implements ListSelectionListener {
    private final JBList<TestCaseDto> list;
    ToolWindow toolWindow = ViewPanel.getToolWindow();

    public SelectionListener(final JBList<TestCaseDto> list) {
        this.list = list;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            TestCaseDto selected = list.getSelectedValue();
            if (selected != null) {

                if (toolWindow != null && toolWindow.isVisible()) {
                    ViewPanel.show(selected);
                }

                //com.intellij.openapi.vcs.changes.ChangeListManager.getInstance(Config.getProject()).getAllChanges().forEach(ContentExtractor::printJsonChanges);
            }

        }
    }
}