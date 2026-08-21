package org.testin.ui.framework;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * One captioned row offering a list of values and accepting one that is not on
 * it — a muted caption and an editable combo box.
 * <p>
 * {@link RadioSelection} is the row for a closed set: three severities, and no
 * fourth. This is the row for an open one, where the list is what exists today
 * and the tester may name something that does not exist yet — a Git branch being
 * the first of them.
 * <p>
 * What comes back is text, always trimmed, and never null: a row the tester
 * cleared reads as empty, which the dialog decides about rather than guarding
 * against.
 */
public final class ChoiceInput implements DialogComponent {

    private final @NotNull JBPanel<?> panel;
    private final @NotNull ComboBox<String> combo;

    ChoiceInput(final @NotNull String caption, final @NotNull List<String> options, final @NotNull String selected) {
        combo = new ComboBox<>(options.toArray(String[]::new));
        combo.setEditable(true);
        combo.setFont(JBFont.label().biggerOn(2f));
        combo.setSelectedItem(selected);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(Captions.panel(caption), BorderLayout.WEST);
        panel.add(combo, BorderLayout.CENTER);
    }

    /**
     * What the row holds: a value picked from the list, or whatever was typed
     * over it. Trimmed, because a branch name with a space around it is a
     * different name to Git and the same one to the tester.
     */
    public @NotNull String getValue() {
        final Object value = combo.getEditor().getItem();
        return Objects.toString(value, "").trim();
    }

    /**
     * Whether the value is one of the offered ones. The caller decides what a
     * new value means — creating a branch is not the same as choosing one.
     */
    public boolean isNew() {
        final String value = getValue();

        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equals(combo.getItemAt(i))) return false;
        }
        return !value.isEmpty();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return combo;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Picking a value is not a submit gesture: the tester still has a
        // message to write and rows to deselect after choosing where it goes.
    }

    @Override
    public boolean wantsFocus() {
        // The dialog opens on its table or its message field. This row is a
        // decision most testers leave alone, so it does not take the caret.
        return false;
    }

    @Override
    public boolean canFillSpace() {
        return false;
    }
}
