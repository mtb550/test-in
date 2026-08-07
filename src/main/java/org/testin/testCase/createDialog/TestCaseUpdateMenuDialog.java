package org.testin.testCase.createDialog;

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
import org.testin.enums.UpdateTestCaseFields;
import org.testin.generateJavaCode.CodeGeneratorDialog;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.testCase.updateDialog.UpdateTestCaseDialog;
import org.testin.util.logger.Logger;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestCaseUpdateMenuDialog {

    private final @NotNull Project project;
    private final @NotNull List<TestCaseDto> items;
    private final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull CodeGeneratorDialog> updatedItems;

    public TestCaseUpdateMenuDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> items, final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull CodeGeneratorDialog> updatedItems) {
        this.project = p;
        this.items = items;
        this.updatedItems = updatedItems;
    }

    public void show() {
        boolean isSingle = items.size() == 1;
        String title = isSingle ? "Update Test Case" : "Update " + items.size() + " Test Cases";

        showMenu(title, selectedItem -> {

            final GeneratorType gt = selectedItem.getGt();
            Logger.trace("Menu item selected -> " + selectedItem.getName() + " | changeType = " + gt);

            if (isSingle) {
                new UpdateTestCaseDialog(project, items.getFirst(), selectedItem, (tc, cg) -> {
                    cg = new CodeGeneratorDialog(gt);
                    cg.setGt(gt);

                    Logger.trace("Single Edit Save -> Injecting changeType " + cg.getGt() + " into UI's CodeGenerator.");
                    updatedItems.accept(items, cg);

                }).show();

            } else {
                selectedItem.getBulkAction().show(project, items, (list, cg) -> {
                    cg = new CodeGeneratorDialog(gt);
                    cg.setGt(gt);


                    Logger.trace("Bulk Edit Save -> Passing main menu CodeGenerator with changeType " + cg.getGt());
                    updatedItems.accept(list, cg);
                });
            }
        });
    }

    private void showMenu(final String title, final Consumer<UpdateTestCaseFields> onSelection) {
        UpdateTestCaseFields[] fields = Arrays.stream(UpdateTestCaseFields.values()).filter(UpdateTestCaseFields::isUpdateMenuItem).toArray(UpdateTestCaseFields[]::new);
        JBList<UpdateTestCaseFields> list = buildMenuList(fields);
        JBPopup popup = buildPopup(title, list);
        registerShortcuts(list, popup, onSelection);
        popup.showCenteredInCurrentWindow(project);
    }

    @NotNull
    private JBList<UpdateTestCaseFields> buildMenuList(final UpdateTestCaseFields[] fields) {
        JBList<UpdateTestCaseFields> list = new JBList<>(fields);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(createCellRenderer());
        return list;
    }

    @NotNull
    private ColoredListCellRenderer<UpdateTestCaseFields> createCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends UpdateTestCaseFields> l, UpdateTestCaseFields val, int i, boolean sel, boolean focus) {
                setIcon(val.getIcon());
                append(val.getName());
                append("   " + val.getShortcutText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                setBorder(JBUI.Borders.empty(6, 12));
            }
        };
    }

    private JBPopup buildPopup(final String title, final JBList<UpdateTestCaseFields> list) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(list), list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();
    }

    private void registerShortcuts(final JBList<UpdateTestCaseFields> list, final JBPopup popup, final Consumer<UpdateTestCaseFields> onSelection) {
        Runnable triggerSelection = () -> {
            if (list.getSelectedValue() != null) {
                onSelection.accept(list.getSelectedValue());
                popup.closeOk(null);
            }
        };

        Arrays.stream(UpdateTestCaseFields.values())
                .filter(UpdateTestCaseFields::isUpdateMenuItem)
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