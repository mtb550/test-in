package testGit.ui;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.DirectoryType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class CreateNodesDialog {

    public static void show(String title, DirectoryType[] items, Predicate<DirectoryType> isDisabled, BiConsumer<String, DirectoryType> onSelected) {

        ExtendableTextField textField = new ExtendableTextField();
        textField.getEmptyText().setText("Name");
        textField.setFont(JBFont.regular());
        textField.setBorder(JBUI.Borders.empty(6, 10));
        textField.putClientProperty("JTextField.Search.noBorderRing", Boolean.TRUE);

        JBList<DirectoryType> list = new JBList<>(items);
        list.setBorder(JBUI.Borders.empty());

        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (index0 >= 0 && index0 < items.length && !isDisabled.test(items[index0])) {
                    super.setSelectionInterval(index0, index1);
                }
            }
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int firstAvailableIndex = getNextEnabledIndex(items, isDisabled, -1, 1);
        if (firstAvailableIndex != -1) {
            list.setSelectedIndex(firstAvailableIndex);
        } else {
            list.clearSelection();
        }

        list.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends DirectoryType> list, DirectoryType value, int index, boolean selected, boolean hasFocus) {
                if (isDisabled.test(value)) {
                    setIcon(IconLoader.getDisabledIcon(value.getIcon()));
                    append(value.getDescription(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    setIcon(value.getIcon());
                    append(value.getDescription());
                }
                setBorder(JBUI.Borders.empty(4, 12));
            }
        });

        Runnable updateIcon = () -> {
            DirectoryType selected = list.getSelectedValue();
            if (selected != null && !isDisabled.test(selected)) {
                textField.setExtensions(new ExtendableTextComponent.Extension() {
                    @Override
                    public Icon getIcon(boolean hovered) {
                        return selected.getIcon();
                    }

                    @Override
                    public boolean isIconBeforeText() {
                        return true;
                    }

                    @Override
                    public int getIconGap() {
                        return JBUI.scale(6);
                    }
                });
                textField.repaint();
            }
        };

        list.addListSelectionListener(e -> updateIcon.run());
        updateIcon.run();

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(textField, BorderLayout.CENTER);
        topPanel.setPreferredSize(new Dimension(JBUI.scale(350), JBUI.scale(32)));
        topPanel.setBorder(JBUI.Borders.empty());

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.add(list, BorderLayout.CENTER);
        listPanel.setBorder(JBUI.Borders.empty());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(listPanel, BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.empty());

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, textField)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int newIdx = getNextEnabledIndex(items, isDisabled, list.getSelectedIndex(), 1);
                    list.setSelectedIndex(newIdx);
                    list.ensureIndexIsVisible(newIdx);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int newIdx = getNextEnabledIndex(items, isDisabled, list.getSelectedIndex(), -1);
                    list.setSelectedIndex(newIdx);
                    list.ensureIndexIsVisible(newIdx);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    submit(textField, list, popup, onSelected);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.cancel();
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    int clickedIndex = list.locationToIndex(e.getPoint());
                    if (clickedIndex >= 0 && !isDisabled.test(items[clickedIndex])) {
                        submit(textField, list, popup, onSelected);
                    }
                }
            }
        });

        popup.showCenteredInCurrentWindow(Config.getProject());

        SwingUtilities.invokeLater(() -> {
            textField.revalidate();
            textField.repaint();
        });
    }

    private static int getNextEnabledIndex(DirectoryType[] items, Predicate<DirectoryType> isDisabled, int currentIdx, int direction) {
        int idx = currentIdx + direction;
        while (idx >= 0 && idx < items.length) {
            if (!isDisabled.test(items[idx])) {
                return idx;
            }
            idx += direction;
        }
        return currentIdx;
    }

    private static void submit(ExtendableTextField textField, JBList<DirectoryType> list, JBPopup popup, BiConsumer<String, DirectoryType> onSelected) {
        String text = textField.getText().trim();
        if (!text.isEmpty() && list.getSelectedValue() != null) {
            onSelected.accept(text, list.getSelectedValue());
            popup.closeOk(null);
        } else {
            textField.requestFocus();
        }
    }
}