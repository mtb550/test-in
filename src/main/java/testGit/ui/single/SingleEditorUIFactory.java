package testGit.ui.single;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.ui.bulk.UpdateField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SingleEditorUIFactory {

    public static final float BASE_FONT_SIZE = JBUI.Fonts.label().getSize2D();
    public static final float TITLE_FONT_SIZE = BASE_FONT_SIZE + 6f;
    public static final float FIELD_FONT_SIZE = BASE_FONT_SIZE + 2f;

    public static ExtendableTextField createTextField(final String placeholder, final Icon icon, final float fontSize) {
        ExtendableTextField textField = new ExtendableTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && hasFocus()) {
                    try {
                        Rectangle2D r = modelToView2D(0);
                        if (r != null) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(UIUtil.getContextHelpForeground());
                            g2.setFont(getFont());
                            FontMetrics fm = g2.getFontMetrics();

                            int x = (int) r.getX() + JBUI.scale(1);
                            int y = (int) r.getY() + fm.getAscent();

                            g2.drawString(placeholder, x, y);
                            g2.dispose();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        Font fieldFont = JBFont.regular().deriveFont(fontSize);
        textField.setFont(fieldFont);
        textField.getEmptyText().setFont(fieldFont);
        textField.getEmptyText().setText(placeholder);
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

    public static void addStepField(final Project project, final JPanel container, final List<TextFieldWithAutoCompletion<String>> stepFields, final String text, final Runnable repackAction, final Set<String> uniqueStepsCache) {
        TextFieldWithAutoCompletionListProvider<String> provider = new TextFieldWithAutoCompletion.StringsCompletionProvider(uniqueStepsCache != null ? uniqueStepsCache : Collections.emptySet(), null);
        TextFieldWithAutoCompletion<String> stepField = new TextFieldWithAutoCompletion<>(project, provider, true, text != null ? text : "");
        stepField.setFont(JBFont.regular().deriveFont(FIELD_FONT_SIZE));
        stepField.setPlaceholder("Step " + (stepFields.size() + 1));
        stepField.setShowPlaceholderWhenFocused(true);
        stepField.setBorder(JBUI.Borders.empty(6, 10));

        JPanel stepRow = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        stepRow.setOpaque(false);
        stepRow.setBorder(JBUI.Borders.emptyBottom(6));

        JLabel removeButton = new JLabel(AllIcons.Actions.Cancel);
        removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeButton.setToolTipText("Remove step");

        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                removeButton.setIcon(AllIcons.General.Remove);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeButton.setIcon(AllIcons.Actions.Cancel);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                container.remove(stepRow);
                stepFields.remove(stepField);

                for (int i = 0; i < stepFields.size(); i++) {
                    stepFields.get(i).setPlaceholder("Step " + (i + 1));
                }

                container.revalidate();
                container.repaint();
                repackAction.run();
            }
        });

        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.setBorder(JBUI.Borders.emptyRight(4));
        buttonWrapper.add(removeButton, BorderLayout.CENTER);

        stepRow.add(stepField, BorderLayout.CENTER);
        stepRow.add(buttonWrapper, BorderLayout.EAST);

        stepFields.add(stepField);
        container.add(stepRow);
    }

    public static JPanel wrapComponent(final JComponent component, final UpdateField field) {
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
        combo.setRenderer(new ColoredListCellRenderer<>() {
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

        Arrays.stream(Groups.values())
                .filter(Groups::isActive)
                .map(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(JBFont.regular().deriveFont(FIELD_FONT_SIZE - 1f));
                    return checkBox;
                })
                .forEach(panel::add);

        return panel;
    }

    public static void addGroups(final JPanel groupsPanel, final List<Groups> groups) {
        if (groups == null || groups.isEmpty()) return;
        for (Component c : groupsPanel.getComponents()) {
            if (c instanceof JBCheckBox checkBox) {
                try {
                    if (groups.contains(Groups.valueOf(checkBox.getText()))) {
                        checkBox.setSelected(true);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public static JPanel createReadOnlyField(final String text, final Icon icon, final float fontSize) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        if (icon != null) {
            JLabel iconLabel = new JLabel(IconLoader.getDisabledIcon(icon));
            iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
            panel.add(iconLabel, BorderLayout.WEST);
        }

        JLabel textLabel = new JLabel(text != null ? text : "");
        textLabel.setFont(JBFont.regular().deriveFont(fontSize));
        textLabel.setForeground(UIUtil.getContextHelpForeground());
        textLabel.setBorder(JBUI.Borders.empty(10, 0));

        panel.add(textLabel, BorderLayout.CENTER);
        return panel;
    }

    public static void registerShortcut(final JComponent component, final CustomShortcutSet shortcutSet, final Runnable action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                action.run();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                if (e.getProject() != null && LookupManager.getInstance(e.getProject()).getActiveLookup() != null) {
                    e.getPresentation().setEnabled(false);
                    return;
                }
                e.getPresentation().setEnabled(true);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(shortcutSet, component);
    }
}