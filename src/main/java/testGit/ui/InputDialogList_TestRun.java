package testGit.ui;

import com.intellij.icons.AllIcons;
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

public class InputDialogList_TestRun {

    public static void show(String title, BiConsumer<String, TemplateItem> onSelected) {
        TemplateItem[] items = {
                new TemplateItem("Package", AllIcons.Nodes.Package, false, DirectoryType.PA),
                new TemplateItem("Test Run", AllIcons.Actions.GroupBy, false, DirectoryType.TR),
        };

        ExtendableTextField textField = new ExtendableTextField();
        textField.getEmptyText().setText("Name");
        textField.setFont(JBFont.regular());
        textField.setBorder(JBUI.Borders.empty(6, 10));
        textField.putClientProperty("JTextField.Search.noBorderRing", Boolean.TRUE);

        JBList<TemplateItem> list = new JBList<>(items);
        list.setBorder(JBUI.Borders.empty());

        // 2. Prevent the mouse/system from selecting disabled items
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (index0 >= 0 && index0 < items.length && !items[index0].disabled()) {
                    super.setSelectionInterval(index0, index1);
                }
            }
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // FIX: Automatically select the first enabled item instead of hardcoding index 0
        int firstAvailableIndex = getNextEnabledIndex(items, -1, 1);
        if (firstAvailableIndex != -1) {
            list.setSelectedIndex(firstAvailableIndex);
        } else {
            list.clearSelection(); // Failsafe in case every single item is disabled
        }

        // 3. Render disabled items as grayed out
        list.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends TemplateItem> list, TemplateItem value, int index, boolean selected, boolean hasFocus) {
                if (value.disabled()) {
                    // Gray out the icon and text
                    setIcon(IconLoader.getDisabledIcon(value.icon));
                    append(value.name, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    setIcon(value.icon);
                    append(value.name);
                }
                setBorder(JBUI.Borders.empty(4, 12));
            }
        });

        Runnable updateIcon = () -> {
            TemplateItem selected = list.getSelectedValue();
            if (selected != null && !selected.disabled()) {
                textField.setExtensions(new ExtendableTextComponent.Extension() {
                    @Override
                    public Icon getIcon(boolean hovered) {
                        return selected.icon;
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

        // 4. Update Keyboard Navigation to skip over disabled items
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int newIdx = getNextEnabledIndex(items, list.getSelectedIndex(), 1);
                    list.setSelectedIndex(newIdx);
                    list.ensureIndexIsVisible(newIdx);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int newIdx = getNextEnabledIndex(items, list.getSelectedIndex(), -1);
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

        // 5. Ensure mouse clicks strictly on disabled items do nothing
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    int clickedIndex = list.locationToIndex(e.getPoint());
                    if (clickedIndex >= 0 && !items[clickedIndex].disabled()) {
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

    // Helper: Finds the next index that isn't disabled, skipping over gray items
    private static int getNextEnabledIndex(TemplateItem[] items, int currentIdx, int direction) {
        int idx = currentIdx + direction;
        while (idx >= 0 && idx < items.length) {
            if (!items[idx].disabled()) {
                return idx;
            }
            idx += direction; // Keep skipping
        }
        return currentIdx; // Stop at boundaries if no more valid items
    }

    private static void submit(ExtendableTextField textField, JBList<TemplateItem> list, JBPopup popup, BiConsumer<String, TemplateItem> onSelected) {
        String text = textField.getText().trim();
        if (!text.isEmpty() && list.getSelectedValue() != null) {
            onSelected.accept(text, list.getSelectedValue());
            popup.closeOk(null);
        } else {
            textField.requestFocus();
        }
    }

    // 1. Added 'disabled' boolean to the model. Created a secondary constructor for convenience.
    public record TemplateItem(String name, Icon icon, boolean disabled, DirectoryType directoryType) {
        public TemplateItem(String name, Icon icon, DirectoryType directoryType) {
            this(name, icon, false, directoryType);
        }
    }
}