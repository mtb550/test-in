package org.testin.ui;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

public class CreateNodesDialog {

    public static void show(final CreateNodeMenu menu, final BiConsumer<String, DirectoryType> onSelected) {
        show(menu, null, onSelected);
    }

    public static void show(final CreateNodeMenu menu, final JComponent settingButton, final BiConsumer<String, DirectoryType> onSelected) {

        ExtendableTextField textField = new ExtendableTextField();
        textField.getEmptyText().setText("Name");

        textField.setBorder(JBUI.Borders.empty(8, 10));
        textField.putClientProperty("JTextField.Search.noBorderRing", Boolean.TRUE);

        DirectoryType[] items = menu.getAvailableOptions();
        JBList<DirectoryType> list = new JBList<>(items);
        list.setBorder(JBUI.Borders.empty());
        list.setFont(textField.getFont());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (items.length > 0) {
            list.setSelectedIndex(0);
        }

        list.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends DirectoryType> list, DirectoryType value, int index, boolean selected, boolean hasFocus) {
                setIcon(value.getIcon());
                String description = value.getDescription() != null ? value.getDescription() : value.name();
                append(description, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                setBorder(JBUI.Borders.empty(4, 12));
            }
        });

        Runnable updateIcon = () -> {
            DirectoryType selected = list.getSelectedValue();
            if (selected != null) {
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
                        return JBUI.scale(10);
                    }
                });
                textField.repaint();
            }
        };

        list.addListSelectionListener(e -> updateIcon.run());
        updateIcon.run();

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(textField, BorderLayout.CENTER);
        topPanel.setPreferredSize(new Dimension(JBUI.scale(350), JBUI.scale(36)));
        topPanel.setBorder(JBUI.Borders.empty());

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.add(list, BorderLayout.CENTER);
        listPanel.setBorder(JBUI.Borders.empty());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);

        if (items.length > 1) {
            mainPanel.add(listPanel, BorderLayout.CENTER);
        }

        mainPanel.setBorder(JBUI.Borders.empty());

        ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, textField)
                .setTitle(menu.getTitle())
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false);

        if (settingButton != null) {
            builder.setSettingButtons(settingButton);
        }

        JBPopup popup = builder.createPopup();

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int currentIdx = list.getSelectedIndex();

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (items.length > 0) {
                        int newIdx = Math.min(items.length - 1, currentIdx + 1);
                        list.setSelectedIndex(newIdx);
                        list.ensureIndexIsVisible(newIdx);
                    }
                    e.consume();

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (items.length > 0) {
                        int newIdx = Math.max(0, currentIdx - 1);
                        list.setSelectedIndex(newIdx);
                        list.ensureIndexIsVisible(newIdx);
                    }
                    e.consume();

                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    submit(textField, list, popup, onSelected, items.length);

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
                    if (clickedIndex >= 0) {
                        submit(textField, list, popup, onSelected, items.length);
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

    private static void submit(final ExtendableTextField textField, final JBList<DirectoryType> list, final JBPopup popup, final BiConsumer<String, DirectoryType> onSelected, final int listSize) {
        String text = textField.getText().trim();

        boolean hasText = !text.isEmpty();
        boolean isValidSelection = (listSize == 0) || (list.getSelectedValue() != null);

        if (hasText && isValidSelection) {
            onSelected.accept(text, list.getSelectedValue());
            popup.closeOk(null);
        } else {
            textField.requestFocus();
        }
    }
}