package testGit.editorPanel.testCaseEditor;


import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import testGit.pojo.Config;
import testGit.pojo.mappers.TestCase;
import testGit.util.ContentExtractor;
import testGit.viewPanel.ViewPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@AllArgsConstructor
public class SelectionListener implements ListSelectionListener {
    private final JBList<TestCase> list;

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            TestCase selected = list.getSelectedValue();
            if (selected != null) {
                ToolWindow toolWindow = ToolWindowManager.getInstance(Config.getProject())
                        .getToolWindow("Details");

                if (toolWindow != null && toolWindow.isVisible()) {
                    ViewPanel.show(selected);
                }

                com.intellij.openapi.vcs.changes.ChangeListManager.getInstance(Config.getProject())
                        .getAllChanges()
                        .forEach(ContentExtractor::printJsonChanges);
            }
        }
    }
}