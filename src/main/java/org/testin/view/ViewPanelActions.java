package org.testin.view;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.statusbar.NextTestCaseAction;
import org.testin.editor.statusbar.PreviousTestCaseAction;

import javax.swing.*;
import java.util.List;

public class ViewPanelActions {

    // UC-VIEW-PANEL-003
    public @NotNull List<AnAction> create(final @NotNull ViewPagination page, final @NotNull JComponent component) {
        return List.of(
                new PreviousTestCaseAction(page, component),
                new NextTestCaseAction(page, component)
        );
    }
}
