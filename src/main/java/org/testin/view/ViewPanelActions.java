package org.testin.view;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.CardHoverAction;
import org.testin.editor.statusbar.NextTestCaseAction;
import org.testin.editor.statusbar.PreviousTestCaseAction;

import javax.swing.JComponent;
import java.util.List;

public class ViewPanelActions {

    /**
     * UC-VIEW-PANEL-003, UC-VIEW-PANEL-012, UC-VIEW-PANEL-014.
     * <p>
     * What the panel answers: the two buttons in its title bar, and the two keys
     * its own tooltips name.
     * <p>
     * The card actions are bound and not returned. Their buttons are already
     * drawn inside the Details tab, tooltips and all - what was missing was the
     * keys those tooltips promised, which were bound on the editor's card list
     * and nowhere else (#225).
     */
    public @NotNull List<AnAction> create(final @NotNull ViewPanel panel, final @NotNull JComponent component) {
        ShownCaseAction.bind(panel, CardHoverAction.NAVIGATE_TO_TEST_METHOD, component);
        ShownCaseAction.bind(panel, CardHoverAction.RUN_TEST_CASE, component);

        return List.of(
                new PreviousTestCaseAction(panel.getPage(), component),
                new NextTestCaseAction(panel.getPage(), component)
        );
    }
}
