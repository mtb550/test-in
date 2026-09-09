package org.testin.view;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.CardHoverAction;

import javax.swing.JComponent;

/**
 * UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-003.
 * <p>
 * One card action, done to the test case the panel is showing, by the key its
 * own tooltip names.
 * <p>
 * The buttons were already there and already correct: {@code ActionIcons} draws
 * them from {@link CardHoverAction} itself, so the icon, the words and the key
 * in the tooltip all come off the action. Only the key was missing - it is bound
 * on the editor's card list and nowhere else, so a tester reading <b>F5</b> in
 * the panel and pressing it got nothing (#225).
 * <p>
 * Bound rather than shown. The panel already draws these two inside the Details
 * tab, and adding them to the tool window's title bar as well would answer a
 * question nobody asked.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class ShownCaseAction extends DumbAwareAction {

    private final @NotNull ViewPanel panel;
    private final @NotNull CardHoverAction action;

    /**
     * Binds the action's own key to the component the panel lives in.
     * <p>
     * The action's, not one chosen here - the same {@link CardHoverAction} that
     * printed the key into the tooltip is asked for it, so the two cannot
     * disagree the way they did.
     */
    static void bind(final @NotNull ViewPanel panel, final @NotNull CardHoverAction action, final @NotNull JComponent component) {
        new ShownCaseAction(panel, action).registerCustomShortcutSet(action.getShortcut().getCustomShortcut(), component);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        panel.getCurrentTestCase().ifPresent(shown -> action.execute(panel.getP(), shown));
    }

    /**
     * Gray with nothing on screen to act on, and gray with the reason when the
     * IDE has not got the plugin the action needs - which is what
     * {@code enableOrExplain} writes into the presentation.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(panel.getCurrentTestCase().isPresent() && action.enableOrExplain(e.getPresentation()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT: the case on display is a field the panel mutates on the EDT, so a
        // background read would enable the key against a case that has gone.
        return ActionUpdateThread.EDT;
    }
}
