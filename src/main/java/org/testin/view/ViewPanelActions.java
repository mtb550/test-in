package org.testin.view;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.statusBar.NextTestCaseAction;
import org.testin.editor.statusBar.PreviousTestCaseAction;

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
