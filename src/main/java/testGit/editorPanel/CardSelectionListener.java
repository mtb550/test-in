package testGit.editorPanel;

import com.intellij.ui.components.JBList;
import testGit.pojo.TestCase;
import testGit.viewPanel.ViewPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Listens for selection changes in the TestCase list and updates the details view.
 */
public class CardSelectionListener implements ListSelectionListener {
    private final JBList<TestCase> list;

    public CardSelectionListener(final JBList<TestCase> list) {
        this.list = list;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // Ensure we only trigger once per selection (not during the drag process)
        if (!e.getValueIsAdjusting()) {
            TestCase selected = list.getSelectedValue();
            if (selected != null) {
                // Auto-update details window on selection change
                ViewPanel.show(selected);
            }
        }
    }
}