package org.testin.testRun.updateDialog;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.RunItemUpdateFields;
import org.testin.mappers.TestRunItems;
import org.testin.util.logger.Logger;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.function.Consumer;

public class RunItemUpdateMenuDialog {

    private final @NotNull Project p;
    private final @NotNull TestRunItems runItem;
    private final @NotNull Consumer<TestRunItems> updatedItem;

    public RunItemUpdateMenuDialog(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull Consumer<TestRunItems> updatedItem) {
        this.p = p;
        this.runItem = runItem;
        this.updatedItem = updatedItem;
    }

    public void show() {
        showMenu(selectedItem -> {
            Logger.trace("Menu item selected -> " + selectedItem.getName());
            new UpdateRunItemDialog(p, runItem, selectedItem, updatedItem).show();
        });
    }

    private void showMenu(final @NotNull Consumer<RunItemUpdateFields> onSelection) {
        final RunItemUpdateFields[] fields = Arrays.stream(RunItemUpdateFields.values())
                .filter(RunItemUpdateFields::isUpdateMenuItem)
                .toArray(RunItemUpdateFields[]::new);

        final JBList<RunItemUpdateFields> list = buildMenuList(fields);
        final JBPopup popup = buildPopup(list);
        registerShortcuts(list, popup, onSelection);
        popup.showCenteredInCurrentWindow(p);
    }

    @NotNull
    private JBList<RunItemUpdateFields> buildMenuList(final RunItemUpdateFields[] fields) {
        final JBList<RunItemUpdateFields> list = new JBList<>(fields);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(createCellRenderer());
        return list;
    }

    @NotNull
    private ColoredListCellRenderer<RunItemUpdateFields> createCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends RunItemUpdateFields> l,
                                                 RunItemUpdateFields val, int i, boolean sel, boolean focus) {
                setIcon(val.getIcon());
                append(val.getName());
                append("   " + val.getShortcutText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                setBorder(JBUI.Borders.empty(6, 12));
            }
        };
    }

    private JBPopup buildPopup(final JBList<RunItemUpdateFields> list) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(list), list)
                .setTitle("Update Test Run Item")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();
    }

    private void registerShortcuts(final JBList<RunItemUpdateFields> list, final JBPopup popup, final Consumer<RunItemUpdateFields> onSelection) {
        final Runnable triggerSelection = () -> {
            if (list.getSelectedValue() != null) {
                onSelection.accept(list.getSelectedValue());
                popup.closeOk(null);
            }
        };

        Arrays.stream(RunItemUpdateFields.values())
                .filter(RunItemUpdateFields::isUpdateMenuItem)
                .forEach(f -> f.bindShortcut(list, () -> {
                    onSelection.accept(f);
                    popup.closeOk(null);
                }));

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                triggerSelection.run();
            }
        }.registerCustomShortcutSet(CommonShortcuts.ENTER, list);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    list.setSelectedIndex(idx);
                    triggerSelection.run();
                }
            }
        });
    }
}
