package testGit.ui.TestCase;

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
import testGit.ui.TestCase.edit.bulk.*;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TestCaseEditMenu {

    public static void show(final List<TestCaseDto> selectedItems, final Consumer<TestCaseDto> onSingleUpdate, final Runnable onBulkUpdate) {
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        if (selectedItems.size() == 1)
            showSingle(selectedItems.getFirst(), onSingleUpdate);
        else
            showBulk(selectedItems, onBulkUpdate);
    }

    private static void showSingle(final TestCaseDto existingDto, final Consumer<TestCaseDto> onUpdate) {
        showMenu("Edit Test Case", selectedField ->
                new EditTestCaseUI().show(existingDto, selectedField, onUpdate));
    }

    private static void showBulk(final List<TestCaseDto> selectedItems, final Runnable onUpdate) {
        String title = "Update " + selectedItems.size() + " Test Cases";

        showMenu(title, selectedField -> {
            switch (selectedField) {
                case PRIORITY -> new PriorityBulkEditor().show(selectedItems, onUpdate);
                case TITLE -> new TitleBulkEditor().show(selectedItems, onUpdate);
                case EXPECTED -> new ExpectedBulkEditor().show(selectedItems, onUpdate);
                case STEPS -> new StepsBulkEditor().show(selectedItems, onUpdate);
                case GROUPS -> new GroupsBulkEditor().show(selectedItems, onUpdate);
                default -> System.out.println("Selected: " + selectedField.getLabel() + " (Not supported for bulk)");
            }
        });
    }

    private static void showMenu(String title, Consumer<EditField> onSelection) {
        EditField[] editableFields = Arrays.stream(EditField.values())
                .filter(EditField::isEditMenuItem)
                .toArray(EditField[]::new);

        JBList<EditField> list = new JBList<>(editableFields);
        list.setBorder(JBUI.Borders.empty(4, 0));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (editableFields.length > 0) list.setSelectedIndex(0);

        list.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends EditField> list, EditField value, int index, boolean selected, boolean hasFocus) {
                if (value.getIcon() != null) {
                    setIcon(value.getIcon());
                }
                append(value.getLabel());

                String shortcutStr = value.getShortcutText();
                char shortcut = shortcutStr.isEmpty() ? ' ' : shortcutStr.charAt(0);
                if (shortcut != ' ') {
                    append("   " + Character.toUpperCase(shortcut), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                setBorder(JBUI.Borders.empty(6, 12));
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(JBUI.Borders.empty());

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, list)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());
                for (EditField item : editableFields) {
                    String shortcutStr = item.getShortcutText();
                    char shortcut = shortcutStr.isEmpty() ? ' ' : shortcutStr.charAt(0);
                    if (shortcut != ' ' && Character.toLowerCase(shortcut) == keyChar) {
                        onSelection.accept(item);
                        popup.cancel();
                        e.consume();
                        return;
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (list.getSelectedValue() != null) {
                        onSelection.accept(list.getSelectedValue());
                        popup.closeOk(null);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.cancel();
                    e.consume();
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    int clickedIndex = list.locationToIndex(e.getPoint());
                    if (clickedIndex >= 0) {
                        onSelection.accept(editableFields[clickedIndex]);
                        popup.closeOk(null);
                    }
                }
            }
        });

        popup.showCenteredInCurrentWindow(Config.getProject());
    }
}