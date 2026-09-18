/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private static final @NotNull String PICK = "testin.choice.pick";

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

        enterPicksOnlyFromTheOpenList();
    }

    /**
     * Rule-INTERNAL-055, Rule-INTERNAL-085.
     * <p>
     * Enter picks the value under it while the list is open, and is the
     * dialog's key the rest of the time.
     * <p>
     * The cursor is in the combo's own text field, not the combo, and that
     * field takes Enter to commit what was typed whether the list is open or
     * not - so the dialog's Enter never arrived, and in Pending Changes a tester
     * who typed a new branch name and pressed Enter got nothing, under a strip
     * reading "Enter - Commit and Push" (#66, finding 186). Handing the field's
     * Enter to the dialog outright would push while the tester was picking a
     * branch from the list. So the field's binding answers only while the list is
     * open; the rest of the time it stands aside and the key reaches the
     * dialog's.
     */
    private void enterPicksOnlyFromTheOpenList() {
        if (!(combo.getEditor().getEditorComponent() instanceof JComponent field)) return;

        final @NotNull KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        final @NotNull Optional<Action> pick = Optional.ofNullable(field.getInputMap().get(enter)).map(key -> field.getActionMap().get(key));
        if (pick.isEmpty()) return;

        field.getInputMap().put(enter, PICK);
        field.getActionMap().put(PICK, new AbstractAction() {
            @Override
            public boolean isEnabled() {
                return combo.isPopupVisible();
            }

            @Override
            public void actionPerformed(final @NotNull ActionEvent e) {
                pick.orElseThrow().actionPerformed(e);
            }
        });
    }

    /**
     * What the row holds: a value picked from the list, or whatever was typed
     * over it. Trimmed, because a branch name with a space around it is a
     * different name to Git and the same one to the tester.
     */
    public @NotNull String getValue() {
        final @NotNull Object value = combo.getEditor().getItem();
        return Objects.toString(value, "").trim();
    }

    /**
     * Whether the value is one of the offered ones. The caller decides what a
     * new value means — creating a branch is not the same as choosing one.
     */
    public boolean isNew() {
        final @NotNull String value = getValue();

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
