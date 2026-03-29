package testGit.ui;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Function;

public class GenericSelectionPopup {

    public static <T> void show(String title,
                                T[] items,
                                Function<T, String> nameFunc,
                                Function<T, Character> shortcutFunc,
                                Function<T, Icon> iconFunc,
                                Consumer<T> onSelected) {

        JBList<T> list = new JBList<>(items);
        list.setBorder(JBUI.Borders.empty(4, 0));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (items.length > 0) {
            list.setSelectedIndex(0);
        }

        list.setCellRenderer(new ColoredListCellRenderer<T>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
                String name = nameFunc.apply(value);
                Character shortcut = shortcutFunc.apply(value);

                if (iconFunc != null) {
                    Icon icon = iconFunc.apply(value);
                    if (icon != null) {
                        setIcon(icon);
                    }
                }

                append(name);

                if (shortcut != null && shortcut != ' ') {
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

                for (T item : items) {
                    Character shortcut = shortcutFunc.apply(item);
                    if (shortcut != null && Character.toLowerCase(shortcut) == keyChar) {
                        onSelected.accept(item);
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
                        onSelected.accept(list.getSelectedValue());
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
                        onSelected.accept(items[clickedIndex]);
                        popup.closeOk(null);
                    }
                }
            }
        });

        popup.showCenteredInCurrentWindow(Config.getProject());
    }
}