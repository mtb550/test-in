package org.testin.viewPanel;

import com.intellij.openapi.actionSystem.AnAction;
import org.testin.editorPanel.statusBar.NextTestCaseAction;
import org.testin.editorPanel.statusBar.PreviousTestCaseAction;

import javax.swing.*;
import java.util.List;

public class ViewPanelActions {

    public List<AnAction> create(final ViewPagination page, final JComponent component) {
        return List.of(
                new PreviousTestCaseAction(page, component),
                new NextTestCaseAction(page, component)
        );
    }
}
