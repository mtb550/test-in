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

package org.testin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.MenuItem;
import org.testin.ui.framework.StatusBarBase;
import org.testin.model.StatusBarItem;
import org.testin.ui.framework.DialogKeys;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.ListValue;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-067.
 * <p>
 * Generic list-menu popup with per-item shortcuts, used by the copy menu, the
 * update menu and the status menu. Selection happens by click, Enter, or an
 * item own keyboard shortcut; the popup closes after invoking the selection
 * callback.
 * <p>
 * Not a framework dialog and does not need to be - a menu is rows and nothing
 * else, and each row already carries its own key and prints it. What it does
 * share is the promise: the keys the menu itself answers come from the one
 * declaration that draws the strip, through {@link DialogKeys} and
 * {@link StatusBarBase} - the same two halves every dialog uses.
 * <p>
 * Enter used to be registered here and printed nowhere. The rows say their own
 * letters, so the single key a tester is most likely to reach for - the one that
 * takes the row already highlighted - was the only one the menu never mentioned
 * (#11).
 */
@RequiredArgsConstructor
public final class ShortcutMenuPopup<T extends MenuItem> {

    private final @NotNull Project p;
    private final @NotNull String title;
    private final T @NotNull [] items;
    private final @NotNull Consumer<T> onSelection;

    // UC-INTERNAL-007, Rule-INTERNAL-053, Rule-INTERNAL-067
    public void show() {
        final @NotNull JBList<T> list = new JBList<>(items);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(new ShortcutMenuRenderer<>());

        DialogStyle.styleContent(list);

        final @NotNull JBPanel<?> content = new JBPanel<>(new BorderLayout());
        content.setOpaque(false);
        content.add(new JBScrollPane(list), BorderLayout.CENTER);

        final @NotNull JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(content, list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        content.add(declaredKeys(list, popup), BorderLayout.SOUTH);
        bindRows(list, popup);

        popup.showCenteredInCurrentWindow(p);
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-067.
     * <p>
     * The keys the menu itself answers - as opposed to the letters its rows
     * carry - declared once and handed to both halves: {@link DialogKeys} binds
     * them and {@link StatusBarBase} draws them.
     * <p>
     * Enter was a {@code registerCustomShortcutSet} of its own here, which
     * worked and said nothing. The arrows bind themselves, the way they do in
     * every list, so they are declared as the hint every other Testin list
     * shows.
     *
     * @return the strip that same declaration renders
     */
    private @NotNull JBPanel<?> declaredKeys(final @NotNull JBList<T> list, final @NotNull JBPopup popup) {
        final @NotNull List<StatusBarShortcut> declared = List.of(
                StatusBarShortcut.confirm(() -> ListValue.selected(list).ifPresent(item -> select(item, popup))),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(popup::cancel));

        DialogKeys.install(list, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, declared);

        return new StatusBarBase(declared.toArray(StatusBarItem[]::new)).getPanel();
    }

    /**
     * What each row answers to: its own letter, and a click anywhere on it.
     * <p>
     * A row key belongs to the row rather than to the popup showing it, which is
     * why {@link MenuItem#bindShortcut} is declared there and asked here.
     */
    private void bindRows(final @NotNull JBList<T> list, final @NotNull JBPopup popup) {
        for (final T item : items) {
            item.bindShortcut(list, () -> select(item, popup));
        }

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                final int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;

                list.setSelectedIndex(idx);
                ListValue.selected(list).ifPresent(item -> select(item, popup));
            }
        });
    }

    private void select(final @NotNull T item, final @NotNull JBPopup popup) {
        onSelection.accept(item);
        popup.closeOk(null);
    }
}
