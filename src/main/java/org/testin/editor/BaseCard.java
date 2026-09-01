package org.testin.editor;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;

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
    /**
     * Which action icon the pointer is over, by name, and empty for none - the
     * card draws one of them larger, and "none" is a state it draws too.
     */
    protected @NotNull String hoveredAction = "";
    /**
     * Which button the card offers in its run slot. Run until something says
     * otherwise, so a card whose state nobody tracks still offers the gesture.
     */
    protected @NotNull CardHoverAction runSlot = CardHoverAction.RUN_TEST_CASE;
    /**
     * The title as words, kept beside the label because a wrapped label holds
     * markup instead - see {@link #layOutTitle}.
     */
    private @NotNull String plainTitle = "";
    /**
     * How wide the title may be drawn before it wraps, and the widest the hover
     * icons may be pushed out. Unbounded until a list says otherwise, so a card
     * measured before it is laid out reads as the one-line card it used to be.
     */
    private int titleColumnWidth = Integer.MAX_VALUE;

    public BaseCard() {
        setLayout(new BorderLayout());
        setOpaque(true);

        descriptionLabel.setForeground(UIUtil.getLabelForeground());

        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final @NotNull JBPanel<?> titleLine = new JBPanel<>();
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
    public static @NotNull String titleText(final int position, final boolean showOrder, final @NotNull String description) {
        final @NotNull String order = showOrder ? String.format(Locale.ENGLISH, "%d.", position) : "";

        return order.isEmpty() || description.isEmpty() ? order + description : order + " " + description;
    }

    /**
     * Lays the card out for the list it is drawn in: the fonts every label takes
     * from the list, and the width the title has before it wraps, which is the
     * list's own less what the card spends on insets and hover icons.
     * <p>
     * After {@code updateData}, and it has to be: the title cannot be laid out
     * until both the font it is measured in and the width it must fit are known,
     * and the font arrives here.
     */
    public void applyListLayout(final @NotNull JList<?> list) {
        final @NotNull Font listFont = list.getFont();
        final float baseSize = listFont.getSize2D();

        descriptionLabel.setFont(listFont.deriveFont(Font.BOLD, baseSize + TITLE_FONT_DELTA));

        for (final JBLabel lbl : attributeLabels.values()) {
            lbl.setFont(listFont.deriveFont(baseSize));
        }

        final float badgeSize = Math.max(8.0f, baseSize - 2.0f);
        for (final Component c : badgePanel.getComponents()) {
            c.setFont(listFont.deriveFont(Font.BOLD, badgeSize));
        }

        titleColumnWidth = Shared.titleColumnWidth(list.getWidth());
        layOutTitle();
    }

    /**
     * Draws the title on one line while it fits, and over as many as it takes
     * when it does not.
     * <p>
     * A label wraps only what it is given as HTML, and only against a width
     * written into the markup - so the wrapping case is the only one that becomes
     * HTML, and a title that fits is set as the plain string it always was. That
     * keeps the common card exactly as it rendered before, and keeps the escaping
     * off every title but the ones that need it: a description is a tester's
     * sentence and may hold a {@code <}, which as markup would swallow the rest
     * of the line.
     */
    private void layOutTitle() {
        final int plainWidth = descriptionLabel.getFontMetrics(descriptionLabel.getFont()).stringWidth(plainTitle);

        if (plainWidth <= titleColumnWidth) {
            descriptionLabel.setText(plainTitle);
            return;
        }

        descriptionLabel.setText("<html><body style='width:" + titleColumnWidth + "px'>" + StringUtil.escapeXmlEntities(plainTitle) + "</body></html>");
    }

    /**
     * Draws the row. The title arrives composed - the editor owns what it reads,
     * because only it knows which attributes are ticked - so neither card decides
     * anything about it here.
     */
    protected void updateUI(final int index, final @NotNull String title, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details) {
        // Kept as it arrived, and put on the label by applyListLayout once the
        // font and the width it must fit are both known. Not set here as well:
        // the label's text is markup when the title wraps, so one method composes
        // it and everything that wants the words reads plainTitle instead.
        plainTitle = title;

        final @NotNull Color currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        Shared.showBadges(badgePanel, badges);

        attributeLabels.values().forEach(lbl -> lbl.setVisible(false));

        details.forEach((attrName, value) -> {
            // A caption with nothing after it says nothing: "Executed At: " on a
            // pending case, "Actual Result: " on a passing one. The framework's
            // details rows drop blank values for the same reason; the card is the
            // one place that decides it for every attribute.
            if (value.isBlank()) return;

            final @NotNull JBLabel lbl = attributeLabels.computeIfAbsent(attrName, k -> {
                final @NotNull JBLabel newLbl = createDetailLabel();
                content.add(newLbl);
                return newLbl;
            });

            lbl.setText(attrName + ": " + value);
            lbl.setVisible(true);
        });

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    public void setActionsState(final boolean isSelected, final boolean isRowHovered, final @NotNull String hoveredAction) {
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
     * <p>
     * Capped at the title column, so a title that wrapped puts the icons at the
     * end of its first line rather than off the card - which is what the width of
     * the whole unwrapped string would ask for.
     */
    public int titleWidth() {
        return Math.min(descriptionLabel.getFontMetrics(descriptionLabel.getFont()).stringWidth(plainTitle), titleColumnWidth);
    }

    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (isRowHovered) {
            Shared.drawDescriptionActionIcons(this, g, titleWidth(), hoveredAction, runSlot);
        }
    }

    private @NotNull JBLabel createDetailLabel() {
        final @NotNull JBLabel label = new JBLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
