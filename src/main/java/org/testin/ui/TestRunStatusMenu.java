package org.testin.ui;

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
import org.testin.pojo.TestRunStatus;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.function.Consumer;

// todo, to be refactored
public class TestRunStatusMenu {

    private final Project project;

    private final Consumer<TestRunStatus> onStatusSelected;

    public TestRunStatusMenu(final @NotNull Project project, final Consumer<TestRunStatus> onStatusSelected) {
        this.project = project;
        this.onStatusSelected = onStatusSelected;
    }

    public void show() {
        TestRunStatus[] statuses = TestRunStatus.values();
        JBList<TestRunStatus> list = buildMenuList(statuses);
        JBPopup popup = buildPopup("Set Test Run Status", list);

        registerShortcuts(list, popup, onStatusSelected);

        popup.showCenteredInCurrentWindow(project);
    }

    @NotNull
    private JBList<TestRunStatus> buildMenuList(final TestRunStatus[] statuses) {
        JBList<TestRunStatus> list = new JBList<>(statuses);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(createCellRenderer());
        return list;
    }

    @NotNull
    private ColoredListCellRenderer<TestRunStatus> createCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends TestRunStatus> l, TestRunStatus val, int i, boolean sel, boolean focus) {
                setIcon(val.getIcon());
                append(val.getLabel());

                append("   " + val.getShortcutText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                setBorder(JBUI.Borders.empty(6, 12));
            }
        };
    }

    private JBPopup buildPopup(final String title, final JBList<TestRunStatus> list) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(list), list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();
    }

    private void registerShortcuts(final JBList<TestRunStatus> list, final JBPopup popup, final Consumer<TestRunStatus> onSelection) {

        Runnable triggerSelection = () -> {
            if (list.getSelectedValue() != null) {
                onSelection.accept(list.getSelectedValue());
                popup.closeOk(null);
            }
        };


        Arrays.stream(TestRunStatus.values())
                .forEach(status -> status.bindShortcut(list, () -> {
                    onSelection.accept(status);
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