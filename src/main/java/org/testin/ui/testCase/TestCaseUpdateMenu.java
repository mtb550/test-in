package org.testin.ui.testCase;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.ui.testCase.update.UpdateTestCaseFields;
import org.testin.ui.testCase.update.UpdateTestCaseUI;
import org.testin.util.autoGenerator.CodeGenerator;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.logger.Log;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestCaseUpdateMenu {

    private final Project project;
    private final List<TestCaseDto> items;
    private final BiConsumer<List<TestCaseDto>, CodeGenerator> updatedItems;

    public TestCaseUpdateMenu(final @NotNull Project project, final List<TestCaseDto> items, final BiConsumer<List<TestCaseDto>, CodeGenerator> updatedItems) {
        this.project = project;
        this.items = items;
        this.updatedItems = updatedItems;
    }

    public void show() {
        boolean isSingle = items.size() == 1;
        String title = isSingle ? "Update Test Case" : "Update " + items.size() + " Test Cases";

        showMenu(title, selectedItem -> {

            final GeneratorType targetChangeType = selectedItem.getChangeType();
            Log.info("TRACE [TestCaseUpdateMenu]: Menu item selected -> " + selectedItem.getName() + " | changeType = " + targetChangeType);

            if (isSingle) {
                new UpdateTestCaseUI(project, items.getFirst(), selectedItem, (tc, codeGenerator) -> {
                    codeGenerator = new CodeGenerator(targetChangeType);
                    codeGenerator.setGeneratorType(targetChangeType);

                    Log.info("TRACE [TestCaseUpdateMenu]: Single Edit Save -> Injecting changeType " + codeGenerator.getGeneratorType() + " into UI's CodeGenerator.");
                    updatedItems.accept(items, codeGenerator);

                }).show();

            } else {
                selectedItem.getBulkAction().show(project, items, (list, codeGenerator) -> {
                    codeGenerator = new CodeGenerator(targetChangeType);
                    codeGenerator.setGeneratorType(targetChangeType);

                    Log.info("TRACE [TestCaseUpdateMenu]: Bulk Edit Save -> Passing main menu CodeGenerator with changeType " + codeGenerator.getGeneratorType());
                    updatedItems.accept(list, codeGenerator);
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