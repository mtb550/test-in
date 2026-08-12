package org.testin.viewPanel;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.statusBar.NextTestCaseAction;
import org.testin.editorPanel.statusBar.PreviousTestCaseAction;

import javax.swing.*;
import java.util.List;

public class ViewPanelActions {

    public @NotNull List<AnAction> create(final @NotNull ViewPagination page, final @NotNull JComponent component) {
        return List.of(
                new PreviousTestCaseAction(page, component),
                new NextTestCaseAction(page, component)
        );
    }
}
