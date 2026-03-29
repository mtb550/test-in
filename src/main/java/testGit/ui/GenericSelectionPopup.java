package testGit.ui;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
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
                                Consumer<T> onSelected) {

        // 1. إنشاء القائمة مباشرة بدون حقل نصي
        JBList<T> list = new JBList<>(items);
        list.setBorder(JBUI.Borders.empty(4, 0));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (items.length > 0) {
            list.setSelectedIndex(0);
        }

        // 2. تصميم شكل العنصر داخل القائمة (الاسم + الاختصار)
        list.setCellRenderer(new ColoredListCellRenderer<T>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
                String name = nameFunc.apply(value);
                Character shortcut = shortcutFunc.apply(value);

                if (shortcut != null && shortcut != ' ') {
                    append(name + " (" + shortcut + ")");
                } else {
                    append(name);
                }
                setBorder(JBUI.Borders.empty(6, 12)); // مسافات مريحة للعين
            }
        });

        // 3. إنشاء الـ Popup وتمرير القائمة كعنصر التركيز الأساسي
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(JBUI.Borders.empty());

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, list) // التركيز يذهب للـ list
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        // 4. التقاط الاختصارات والضغطات من الكيبورد
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char keyChar = e.getKeyChar();
                // فحص الاختصارات (حساس لحالة الأحرف)
                for (T item : items) {
                    Character shortcut = shortcutFunc.apply(item);
                    if (shortcut != null && shortcut.equals(keyChar)) {
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

        // 5. التقاط الضغط بالماوس
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

        // 6. عرض النافذة
        popup.showCenteredInCurrentWindow(Config.getProject());
    }
}