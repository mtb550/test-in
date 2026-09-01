package org.testin.search;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Opens the search, from wherever the tester is (#29).
 * <p>
 * Registered in {@code plugin.xml} rather than bound to a component, and that is
 * the whole point of it. Every other shortcut in the plugin is registered on the
 * thing it acts on - the tree, a list, a table - so it works there and nowhere
 * else, and switching view takes it out of the component tree and the key goes
 * quiet. A search that only worked while the tree had focus would be a search
 * nobody reached for.
 * <p>
 * So the keystroke lives in the keymap, which is also where a tester changes it
 * when it collides with something they already use.
 */
public final class SearchAction extends DumbAwareAction {

    private static final @NotNull String ID = "Testin.Search";

    /**
     * The one registered instance, for a toolbar that wants a button on it.
     * <p>
     * Fetched by id rather than constructed, so the button is the action and not
     * a second copy of it: its text, its description and its icon are the ones
     * declared in {@code plugin.xml}, and its tooltip carries whatever keystroke
     * the keymap currently holds - including one the tester rebound. A
     * {@code new SearchAction()} would be unregistered, so the platform could
     * not name a shortcut for it and the button would quietly disagree with the
     * key (#29).
     */
    public static @NotNull AnAction registered() {
        return Objects.requireNonNull(ActionManager.getInstance().getAction(ID), ID + " is not registered");
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Optional.ofNullable(e.getProject()).ifPresent(p -> new SearchDialog(p).show());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // A project, and nothing more. Whether anything is indexed is the
        // search's answer to give - "nothing matched" is a true and useful
        // reply, and a key that silently does nothing is not.
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT: update() reads no Swing state (#52).
        return ActionUpdateThread.BGT;
    }
}
