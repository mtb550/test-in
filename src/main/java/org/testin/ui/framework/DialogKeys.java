package org.testin.ui.framework;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-056.
 * <p>
 * How a status bar declaration becomes keys a component answers to.
 * <p>
 * The other half of {@link StatusBarBase}, which turns the same list into the
 * strip a tester reads. Both halves take the one declaration, which is the whole
 * of the promise this framework makes: a key cannot be shown without working, or
 * work without being shown (#11).
 * <p>
 * Its own class because there are two surfaces that make that promise and only
 * one of them is a framework dialog. {@code ShortcutMenuPopup} is a list menu
 * rather than a dialog, and it registered Enter privately while printing
 * nothing - so the one key it bound was the one key it never said. A second
 * copy of this loop over there would have been the same divergence written down
 * twice instead of once.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogKeys {

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-055, Rule-INTERNAL-056.
     * <p>
     * Binds every bindable entry of a declaration onto one component. Called
     * once per component that should answer the keys - the condition says
     * whether it answers for itself or for whatever is focused inside it.
     * <p>
     * Two entries on one key would silently shadow each other, so that is a
     * failure rather than a preference: the framework consults one of them and
     * neither end says which.
     */
    public static void install(final @NotNull JComponent target, final @MagicConstant(intValues = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_IN_FOCUSED_WINDOW}) int condition, final @NotNull List<StatusBarShortcut> declared) {
        final @NotNull Set<KeyStroke> bound = new HashSet<>();

        for (int i = 0; i < declared.size(); i++) {
            final @NotNull StatusBarShortcut shortcut = declared.get(i);
            if (!shortcut.isBindable()) continue;

            // isBindable guarantees both; requireNonNull makes that visible to dataflow.
            final @NotNull KeyStroke key = Objects.requireNonNull(shortcut.shortcut()).getKey();
            final @NotNull Runnable action = Objects.requireNonNull(shortcut.action());

            if (!bound.add(key)) {
                throw new IllegalStateException("Duplicate dialog shortcut: " + shortcut.getShortcutText());
            }

            // The index rather than the name: the same entry installed on
            // several components has to reach one action map entry, and two
            // entries called Confirm on one dialog are already refused above.
            installKey(target, condition, key, "testin.framework.shortcut." + i, action);
        }
    }

    private static void installKey(final @NotNull JComponent component, final @MagicConstant(intValues = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_IN_FOCUSED_WINDOW}) int condition, final @NotNull KeyStroke key, final @NotNull String actionKey, final @NotNull Runnable action) {
        component.getInputMap(condition).put(key, actionKey);
        component.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                action.run();
            }
        });
    }
}
