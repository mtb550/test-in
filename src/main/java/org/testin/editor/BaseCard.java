package org.testin.editor;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseCard extends JBPanel<BaseCard> {
    /**
     * Size delta of the card title over the list font. Shared with the mouse hit-testing
     * in {@code CardMouseListener} so hover targets line up with the painted icons.
     */
    public static final float TITLE_FONT_DELTA = 3.0f;
    protected final @NotNull JBLabel descriptionLabel = new JBLabel();
    protected final @NotNull JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    protected final @NotNull Map<String, JBLabel> attributeLabels = new HashMap<>();
    protected final @NotNull JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
    protected final @NotNull BorderLayoutPanel wrapper = new BorderLayoutPanel();
    protected boolean isRowHovered;
    protected @Nullable String hoveredAction;
    protected boolean isRunning;

    public BaseCard() {
        setLayout(new BorderLayout());
        setOpaque(true);

        descriptionLabel.setForeground(UIUtil.getLabelForeground());

        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(descriptionLabel);

        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine);
        content.add(badgePanel);

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);

        add(wrapper, BorderLayout.CENTER);
    }

    public void applyListFont(final @NotNull Font listFont) {
        final float baseSize = listFont.getSize2D();

        descriptionLabel.setFont(listFont.deriveFont(Font.BOLD, baseSize + TITLE_FONT_DELTA));

        for (final JBLabel lbl : attributeLabels.values()) {
            lbl.setFont(listFont.deriveFont(baseSize));
        }

        final float badgeSize = Math.max(8.0f, baseSize - 2.0f);
        for (final Component c : badgePanel.getComponents()) {
            c.setFont(listFont.deriveFont(Font.BOLD, badgeSize));
        }
    }

    protected void updateUI(final int index, final @NotNull String title, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details) {
        descriptionLabel.setText(String.format(Locale.ENGLISH, "%d. %s", index + 1, title));

        final Color currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        badgePanel.removeAll();
        badges.forEach(badgePanel::add);

        attributeLabels.values().forEach(lbl -> lbl.setVisible(false));

        details.forEach((attrName, value) -> {
            final JBLabel lbl = attributeLabels.computeIfAbsent(attrName, k -> {
                final JBLabel newLbl = createDetailLabel();
                content.add(newLbl);
                return newLbl;
            });

            lbl.setText(attrName + ": " + value);
            lbl.setVisible(true);
        });

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    public void setActionsState(final boolean isSelected, final boolean isRowHovered, final @Nullable String hoveredAction) {
        this.isRowHovered = isRowHovered;
        this.hoveredAction = hoveredAction;
        if (isSelected) {
            setBackground(EditorColors.SELECTION_BACKGROUND);
        }
    }

    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (isRowHovered) {
            final FontMetrics fm = descriptionLabel.getFontMetrics(descriptionLabel.getFont());
            final int titleWidth = fm.stringWidth(descriptionLabel.getText());
            Shared.drawDescriptionActionIcons(this, g, titleWidth, JBUI.scale(12), hoveredAction, isRunning);
        }
    }

    private @NotNull JBLabel createDetailLabel() {
        final JBLabel label = new JBLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
