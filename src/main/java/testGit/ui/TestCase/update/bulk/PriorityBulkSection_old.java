//package testGit.ui.TestCase.edit.bulk;
//
//import com.intellij.openapi.ui.popup.JBPopup;
//import com.intellij.openapi.ui.popup.JBPopupFactory;
//import com.intellij.ui.ColoredListCellRenderer;
//import com.intellij.ui.SimpleTextAttributes;
//import com.intellij.ui.components.JBList;
//import com.intellij.ui.components.JBScrollPane;
//import com.intellij.util.ui.JBUI;
//import org.jetbrains.annotations.NotNull;
//import testGit.pojo.Config;
//import testGit.pojo.Priority;
//import testGit.pojo.dto.TestCaseDto;
//import testGit.repository.PersistenceManager;
//
//import javax.swing.*;
//import java.awt.event.KeyAdapter;
//import java.awt.event.KeyEvent;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.List;
//
//public class PriorityBulkEditor_old {
//
//    public void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
//        Priority[] items = Priority.values();
//
//        JBList<Priority> list = new JBList<>(items);
//        list.setBorder(JBUI.Borders.empty(4, 0));
//        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        if (items.length > 0) list.setSelectedIndex(0);
//
//        list.setCellRenderer(new ColoredListCellRenderer<>() {
//            @Override
//            protected void customizeCellRenderer(@NotNull JList<? extends Priority> list, Priority value, int index, boolean selected, boolean hasFocus) {
//                setIcon(value.getIcon());
//                append(value.name());
//
//                char shortcut = value.name().charAt(0);
//                append("   " + Character.toUpperCase(shortcut), SimpleTextAttributes.GRAYED_ATTRIBUTES);
//                setBorder(JBUI.Borders.empty(6, 12));
//            }
//        });
//
//        JBScrollPane scrollPane = new JBScrollPane(list);
//        scrollPane.setBorder(JBUI.Borders.empty());
//
//        JBPopup popup = JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(scrollPane, list)
//                .setTitle("Select Priority")
//                .setRequestFocus(true)
//                .setCancelOnClickOutside(true)
//                .setMovable(false)
//                .createPopup();
//
//        list.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyTyped(KeyEvent e) {
//                char keyChar = Character.toLowerCase(e.getKeyChar());
//                for (Priority item : items) {
//                    char shortcut = item.name().charAt(0);
//                    if (Character.toLowerCase(shortcut) == keyChar) {
//                        PersistenceManager.updatePriority(selectedItems, item, onUpdate);
//                        System.out.println("Priority updated to " + item + " for " + selectedItems.size() + " test cases.");
//                        popup.cancel();
//                        e.consume();
//                        return;
//                    }
//                }
//            }
//
//            @Override
//            public void keyPressed(KeyEvent e) {
//                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//                    if (list.getSelectedValue() != null) {
//                        Priority selectedPriority = list.getSelectedValue();
//                        PersistenceManager.updatePriority(selectedItems, selectedPriority, onUpdate);
//                        System.out.println("Priority updated to " + selectedPriority + " for " + selectedItems.size() + " test cases.");
//                        popup.closeOk(null);
//                    }
//                    e.consume();
//                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
//                    popup.cancel();
//                    e.consume();
//                }
//            }
//        });
//
//        list.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
//                    int clickedIndex = list.locationToIndex(e.getPoint());
//                    if (clickedIndex >= 0) {
//                        Priority selectedPriority = items[clickedIndex];
//                        PersistenceManager.updatePriority(selectedItems, selectedPriority, onUpdate);
//                        System.out.println("Priority updated to " + selectedPriority + " for " + selectedItems.size() + " test cases.");
//                        popup.closeOk(null);
//                    }
//                }
//            }
//        });
//
//        popup.showCenteredInCurrentWindow(Config.getProject());
//    }
//}