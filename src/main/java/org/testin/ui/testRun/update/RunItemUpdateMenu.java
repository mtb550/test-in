package org.testin.ui.testRun.update;

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
import org.testin.pojo.TestRunItems;
import org.testin.util.logger.Log;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.function.Consumer;

public class RunItemUpdateMenu {

    private final Project project;
    private final TestRunItems runItem;
    private final Consumer<TestRunItems> updatedItem;

    public RunItemUpdateMenu(final @NotNull Project project, final @NotNull TestRunItems runItem,
                             final @NotNull Consumer<TestRunItems> updatedItem) {
        this.project = project;
        this.runItem = runItem;
        this.updatedItem = updatedItem;
    }

    public void show() {
        showMenu("Update Test Run Item", selectedItem -> {
            Log.trace("Menu item selected -> " + selectedItem.getName());

            new RunItemUpdateUI(project, runItem, selectedItem, updatedItem).show();
        });
    }

    private void showMenu(final String title, final Consumer<RunItemUpdateFields> onSelection) {
        RunItemUpdateFields[] fields = Arrays.stream(RunItemUpdateFields.values())
                .filter(RunItemUpdateFields::isUpdateMenuItem)
                .toArray(RunItemUpdateFields[]::new);
        JBList<RunItemUpdateFields> list = buildMenuList(fields);
        JBPopup popup = buildPopup(title, list);
        registerShortcuts(list, popup, onSelection);
        popup.showCenteredInCurrentWindow(project);
    }

    @NotNull
    private JBList<RunItemUpdateFields> buildMenuList(final RunItemUpdateFields[] fields) {
        JBList<RunItemUpdateFields> list = new JBList<>(fields);
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

    private JBPopup buildPopup(final String title, final JBList<RunItemUpdateFields> list) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(list), list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();
    }

    private void registerShortcuts(final JBList<RunItemUpdateFields> list, final JBPopup popup,
                                   final Consumer<RunItemUpdateFields> onSelection) {
        Runnable triggerSelection = () -> {
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
