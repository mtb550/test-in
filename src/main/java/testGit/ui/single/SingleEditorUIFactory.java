package testGit.ui.single;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.ui.bulk.UpdateField;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SingleEditorUIFactory {

    public static final float BASE_FONT_SIZE = JBUI.Fonts.label().getSize2D();
    public static final float TITLE_FONT_SIZE = BASE_FONT_SIZE + 6f;
    public static final float FIELD_FONT_SIZE = BASE_FONT_SIZE + 2f;

    public static ExtendableTextField createTextField(String placeholder, Icon icon, float fontSize) {
        ExtendableTextField textField = new ExtendableTextField();
        Font fieldFont = JBFont.regular().deriveFont(fontSize);
        textField.setFont(fieldFont);
        textField.getEmptyText().setFont(fieldFont);
        textField.getEmptyText().setText(placeholder);
        textField.putClientProperty("JTextField.Search.noBorderRing", Boolean.TRUE);
        textField.setBorder(JBUI.Borders.empty(10));
        textField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return icon;
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(8);
            }
        });
        return textField;
    }

    public static void addStepField(JPanel container, List<ExtendableTextField> stepFields, String text, Runnable repackAction) {
        ExtendableTextField stepField = createTextField("Step " + (stepFields.size() + 1), UpdateField.STEPS.getIcon(), FIELD_FONT_SIZE);
        stepField.setText(text);
        stepField.addExtension(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return hovered ? AllIcons.Actions.Cancel : AllIcons.General.Remove;
            }

            @Override
            public boolean isIconBeforeText() {
                return false;
            }

            @Override
            public String getTooltip() {
                return "Remove step";
            }

            @Override
            public Runnable getActionOnClick() {
                return () -> {
                    container.remove(stepField);
                    stepFields.remove(stepField);
                    for (int i = 0; i < stepFields.size(); i++) {
                        stepFields.get(i).getEmptyText().setText("Step " + (i + 1));
                    }
                    container.revalidate();
                    container.repaint();
                    repackAction.run();
                };
            }
        });
        stepFields.add(stepField);
        container.add(stepField);
        container.add(Box.createVerticalStrut(JBUI.scale(4)));
    }

    public static JPanel wrapComponent(JComponent component, UpdateField field) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        if (field != null && field.getIcon() != null) {
            JLabel iconLabel = new JLabel(field.getIcon());
            iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
            wrapper.add(iconLabel, BorderLayout.WEST);
        }
        wrapper.add(component, BorderLayout.CENTER);
        wrapper.setBorder(JBUI.Borders.emptyTop(8));
        return wrapper;
    }

    public static ComboBox<Priority> createPriorityCombo() {
        ComboBox<Priority> combo = new ComboBox<>(Priority.values());
        combo.setFont(JBFont.regular().deriveFont(FIELD_FONT_SIZE));
        combo.setRenderer(new ColoredListCellRenderer<Priority>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Priority> list, Priority value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    setIcon(value.getIcon());
                    append(" Priority: " + value.name());
                }
            }
        });
        return combo;
    }

    public static JPanel createGroupsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        panel.setOpaque(false);

        java.util.Arrays.stream(Groups.values())
                .filter(Groups::isActive)
                .map(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(JBFont.regular().deriveFont(FIELD_FONT_SIZE - 1f));
                    return checkBox;
                })
                .forEach(panel::add);

        return panel;
    }

    public static void prefillGroups(JPanel groupsPanel, List<Groups> groups) {
        if (groups == null || groups.isEmpty()) return;
        for (Component c : groupsPanel.getComponents()) {
            if (c instanceof JBCheckBox checkBox) {
                if (groups.contains(Groups.valueOf(checkBox.getText()))) {
                    checkBox.setSelected(true);
                }
            }
        }
    }

    public static JPanel createReadOnlyField(String text, Icon icon, float fontSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        if (icon != null) {
            JLabel iconLabel = new JLabel(IconLoader.getDisabledIcon(icon));
            iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
            panel.add(iconLabel, BorderLayout.WEST);
        }

        JLabel textLabel = new JLabel(text != null ? text : "");
        textLabel.setFont(JBFont.regular().deriveFont(fontSize));
        textLabel.setForeground(com.intellij.util.ui.UIUtil.getContextHelpForeground());
        textLabel.setBorder(JBUI.Borders.empty(10, 0));

        panel.add(textLabel, BorderLayout.CENTER);
        return panel;
    }

    public static void registerShortcut(JComponent component, int keyCode, int modifiers, Runnable action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                action.run();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(keyCode, modifiers)), component);
    }
}