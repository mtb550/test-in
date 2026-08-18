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

    /**
     * The title line: the Order and Description attributes drawn together, as in
     * "1. Log in with a valid user". Either half can be switched off in the
     * Details popup - an unticked Order drops the number, an unticked Description
     * arrives as an empty {@code description} - so the space between them belongs
     * to whichever pair survives.
     * <p>
     * Static because two callers need the same answer: the card draws it, and the
     * mouse listener measures it to know where the hover icons start. Composing it
     * twice is how they drift.
     */
    public static @NotNull String titleText(final int index, final boolean showOrder, final @NotNull String description) {
        final String order = showOrder ? String.format(Locale.ENGLISH, "%d.", index + 1) : "";

        return order.isEmpty() || description.isEmpty() ? order + description : order + " " + description;
    }

    /**
     * Draws the row. The title arrives composed - the editor owns what it reads,
     * because only it knows which attributes are ticked - so neither card decides
     * anything about it here.
     */
    protected void updateUI(final int index, final @NotNull String title, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details) {
        descriptionLabel.setText(title);

        final Color currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        badgePanel.removeAll();
        badges.forEach(badgePanel::add);

        attributeLabels.values().forEach(lbl -> lbl.setVisible(false));

        details.forEach((attrName, value) -> {
            // A caption with nothing after it says nothing: "Executed At: " on a
            // pending case, "Actual Result: " on a passing one. The framework's
            // details rows drop blank values for the same reason; the card is the
            // one place that decides it for every attribute.
            if (value.isBlank()) return;

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

    /**
     * Width of the title line exactly as it is drawn, which is where the hover
     * icons begin. Owned here because the text is composed here: either half of
     * it can be switched off in the Details popup, so anything that rebuilds the
     * string to measure it drifts away from what is on screen.
     */
    public int titleWidth() {
        return descriptionLabel.getFontMetrics(descriptionLabel.getFont()).stringWidth(descriptionLabel.getText());
    }

    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (isRowHovered) {
            Shared.drawDescriptionActionIcons(this, g, titleWidth(), JBUI.scale(12), hoveredAction, isRunning);
        }
    }

    private @NotNull JBLabel createDetailLabel() {
        final JBLabel label = new JBLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
