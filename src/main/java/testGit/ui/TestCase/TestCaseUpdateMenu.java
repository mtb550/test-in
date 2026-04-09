package testGit.ui.TestCase;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.update.UpdateTestCaseFields;
import testGit.ui.TestCase.update.UpdateTestCaseUI;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TestCaseUpdateMenu {

    public void show(final List<TestCaseDto> items, final Consumer<List<TestCaseDto>> updatedItems) {
        boolean isSingle = items.size() == 1;
        String title = isSingle ? "Edit Test Case" : "Edit " + items.size() + " Test Cases";

        showMenu(title, field -> {
            if (isSingle)
                new UpdateTestCaseUI().show(items.getFirst(), field, tc -> updatedItems.accept(items));
            else
                field.getBulkAction().show(items, updatedItems);
        });
    }

    private void showMenu(final String title, final Consumer<UpdateTestCaseFields> onSelection) {
        UpdateTestCaseFields[] fields = Arrays.stream(UpdateTestCaseFields.values()).filter(UpdateTestCaseFields::isEditMenuItem).toArray(UpdateTestCaseFields[]::new);
        JBList<UpdateTestCaseFields> list = buildMenuList(fields);
        JBPopup popup = buildPopup(title, list);
        registerShortcuts(list, popup, onSelection);
        popup.showCenteredInCurrentWindow(Config.getProject());
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
                append(val.getLabel());
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
                .filter(UpdateTestCaseFields::isEditMenuItem)
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