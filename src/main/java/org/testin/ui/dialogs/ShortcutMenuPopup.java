package org.testin.ui.dialogs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic list-menu popup with per-item shortcuts, used by the update-menu and
 * status-menu dialogs. Selection happens by click, Enter, or an item's own
 * keyboard shortcut; the popup closes after invoking the selection callback.
 */
public final class ShortcutMenuPopup<T> {

    private final @NotNull Project p;
    private final @NotNull String title;
    private final T @NotNull [] items;
    private final @NotNull Function<T, Icon> icon;
    private final @NotNull Function<T, String> label;
    private final @NotNull Function<T, String> shortcutText;
    private final @NotNull ItemShortcutBinder<T> shortcutBinder;
    private final @NotNull Consumer<T> onSelection;

    public ShortcutMenuPopup(final @NotNull Project p,
                             final @NotNull String title,
                             final T @NotNull [] items,
                             final @NotNull Function<T, Icon> icon,
                             final @NotNull Function<T, String> label,
                             final @NotNull Function<T, String> shortcutText,
                             final @NotNull ItemShortcutBinder<T> shortcutBinder,
                             final @NotNull Consumer<T> onSelection) {
        this.p = p;
        this.title = title;
        this.items = items;
        this.icon = icon;
        this.label = label;
        this.shortcutText = shortcutText;
        this.shortcutBinder = shortcutBinder;
        this.onSelection = onSelection;
    }

    public void show() {
        final JBList<T> list = new JBList<>(items);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(new ShortcutMenuRenderer<>(icon, label, shortcutText));

        DialogStyle.styleContent(list);
        final JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(list), list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        registerShortcuts(list, popup);
        popup.showCenteredInCurrentWindow(p);
    }

    private void registerShortcuts(final JBList<T> list, final JBPopup popup) {
        final Runnable triggerSelection = () -> {
            if (list.getSelectedValue() != null) {
                select(list.getSelectedValue(), popup);
            }
        };

        for (final T item : items) {
            shortcutBinder.bind(item, list, () -> select(item, popup));
        }

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                triggerSelection.run();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(CommonShortcuts.ENTER, list);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    list.setSelectedIndex(idx);
                    triggerSelection.run();
                }
            }
        });
    }

    private void select(final T item, final JBPopup popup) {
        onSelection.accept(item);
        popup.closeOk(null);
    }
}
