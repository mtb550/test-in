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
import testGit.ui.TestCase.edit.EditField;
import testGit.ui.TestCase.edit.EditTestCaseUI;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TestCaseEditMenu {

    public void show(final List<TestCaseDto> items, final Consumer<List<TestCaseDto>> updatedItems) {
        boolean isSingle = items.size() == 1;
        String title = isSingle ? "Edit Test Case" : "Edit " + items.size() + " Test Cases";
        showMenu(title, field -> {
            if (isSingle)
                new EditTestCaseUI().show(items.getFirst(), field, tc -> updatedItems.accept(items));
            else
                field.getBulkAction().show(items, updatedItems);
        });
    }

    private void showMenu(String title, Consumer<EditField> onSelection) {
        EditField[] fields = Arrays.stream(EditField.values()).filter(EditField::isEditMenuItem).toArray(EditField[]::new);
        JBList<EditField> list = buildMenuList(fields);
        JBPopup popup = buildPopup(title, list);
        registerShortcuts(list, fields, popup, onSelection);
        registerMouseClick(list, fields, popup, onSelection);
        popup.showCenteredInCurrentWindow(Config.getProject());
    }

    @NotNull
    private JBList<EditField> buildMenuList(EditField[] fields) {
        JBList<EditField> list = new JBList<>(fields);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(createCellRenderer());
        return list;
    }

    @NotNull
    private ColoredListCellRenderer<EditField> createCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends EditField> l, EditField val, int i, boolean sel, boolean focus) {
                setIcon(val.getIcon());
                append(val.getLabel());
                append("   " + val.getShortcutText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                setBorder(JBUI.Borders.empty(6, 12));
            }
        };
    }

    private JBPopup buildPopup(String title, JBList<EditField> list) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(list), list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();
    }

    private void registerShortcuts(JBList<EditField> list, EditField[] fields, JBPopup popup, Consumer<EditField> onSelection) {
        for (EditField f : fields) {
            if (f.getShortcut() != null) {
                new DumbAwareAction() {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        onSelection.accept(f);
                        popup.closeOk(null);
                    }
                }.registerCustomShortcutSet(f.getShortcut().getShortcut(), list);
            }
        }

        // Enter
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (list.getSelectedValue() != null) {
                    onSelection.accept(list.getSelectedValue());
                    popup.closeOk(null);
                }
            }
        }.registerCustomShortcutSet(CommonShortcuts.ENTER, list);
    }

    private void registerMouseClick(JBList<EditField> list, EditField[] fields, JBPopup popup, Consumer<EditField> onSelection) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    onSelection.accept(fields[idx]);
                    popup.closeOk(null);
                }
            }
        });
    }
}