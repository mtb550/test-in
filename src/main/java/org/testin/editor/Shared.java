package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.CardHoverAction;
import org.testin.model.Group;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
// Explicit, because java.awt.* above also offers a List and it takes no type
// parameter - the resulting error names the wrong thing and cascades.
import java.util.List;

public class Shared {

    // ---------------------------------------------------------------- badges
    //
    // One place for badges, look and content together: the constants below are
    // the whole design, the factories say what each badge shows and which colour
    // it takes, and Badge at the bottom paints it. Every badge anywhere in the
    // plugin comes from here - the run card, the test card and the view panel's
    // details - so a change to the look lands on all three at once and none of
    // them can drift. Nothing outside this file builds one.
    //
    // Captioned or bare: a badge is captioned when another badge could be
    // mistaken for it - the test case's priority and the bug's priority declare
    // the same three colours, and severity sits beside both. A badge whose word
    // is its own explanation stays bare.

    private static final int BADGE_RADIUS = 20;

    /** Room around the text, inside the pill. */
    private static final int BADGE_PAD_V = 2;
    private static final int BADGE_PAD_H = 10;

    /**
     * Text for a pill light enough that white would disappear on it. Not a
     * JBColor: the background it sits on is the badge's own, chosen by the value
     * being shown, so it does not follow the editor theme and neither should
     * this.
     */
    private static final @NotNull Color TEXT_ON_LIGHT = Gray._30;

    /**
     * The only colour a badge names here. Every other one comes from the enum of
     * the value being shown, which is where a colour belongs; this one describes
     * a position in the tree rather than a value, so it has no enum to live in.
     */
    private static final @NotNull JBColor UNSORTED_COLOR = new JBColor(new Color(255, 100, 100), new Color(130, 50, 50));

    public static @NotNull JBLabel createPriorityBadge(final @NotNull TestCaseDto tc) {
        return captioned("Priority", tc.getPriority().getName(), tc.getPriority().getColor());
    }

    /**
     * The test case is not under a test set, so it is not in a run yet.
     */
    public static @NotNull JBLabel createUnsortedBadge() {
        return new Badge("Unsorted", UNSORTED_COLOR);
    }

    /**
     * The live state of a case while a run is executing - the label and colour
     * are the status's own.
     */
    public static @NotNull JBLabel createRunStatusBadge(final @NotNull RunStatus.Badge badge) {
        return new Badge(badge.label(), badge.color());
    }

    /**
     * Adds a captioned pill, and adds nothing when there is no value to show.
     * <p>
     * The caption is what makes the row readable: the test case's priority and
     * the bug's priority declare the same three colours, so an uncaptioned
     * "Low" beside another "Low" says two different things in the same pill.
     * <p>
     * The blank rule lives here for the same reason {@code BaseCard} owns it for
     * detail rows - a case that never failed has no severity and no bug
     * priority, and an empty pill is worse than no pill. One place decides it,
     * so every caller stays unconditional.
     */
    public static void addBadge(final @NotNull List<JComponent> badges, final @NotNull String caption,
                                final @NotNull String value, final @NotNull Color color) {
        if (value.isBlank()) return;

        badges.add(captioned(caption, value, color));
    }

    private static @NotNull Badge captioned(final @NotNull String caption, final @NotNull String value,
                                            final @NotNull Color color) {
        return new Badge(caption + ": " + value, color);
    }

    public static @NotNull JBLabel createGroupBadge(final @NotNull Group group) {
        return new Badge(group.getName(), JBColor.darkGray);
    }

    /**
     * Rec. 709 luma: green dominates what the eye reads as brightness, which is
     * why a pure yellow needs dark text and a pure blue does not.
     */
    // Package-private so the contrast rule can be pinned by a test without
    // building a component, which needs the platform's fonts.
    static boolean isLight(final @NotNull Color bg) {
        return 0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue() > 140;
    }

    /**
     * The pill: a rounded label that draws its own background and picks its own
     * text colour. Private, because a badge is asked for by name above and never
     * assembled by a caller - that is what keeps the look in one place.
     */
    private static final class Badge extends JBLabel {

        private Badge(final @NotNull String text, final @NotNull Color bg) {
            super(text);
            setOpaque(false);
            setBackground(bg);

            final float badgeSize = Math.max(8.0f, FontSync.getBaseFontSize() - 2.0f);
            setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD, badgeSize));

            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H));
        }

        /**
         * Derived rather than set, because the badge is the only thing that knows
         * what it is drawn on. White was hard-coded, which is fine on the deep
         * reds and greys the cards started with and unreadable the moment a value
         * takes a light colour - a yellow severity pill with white text says
         * nothing. Computed per call rather than in the constructor because a
         * JBColor resolves to a different value in the dark theme, and the answer
         * has to follow it.
         */
        @Override
        public Color getForeground() {
            final Color bg = getBackground();
            if (bg == null) return super.getForeground();

            return isLight(bg) ? TEXT_ON_LIGHT : JBColor.WHITE;
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), BADGE_RADIUS, BADGE_RADIUS);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    public static void drawDescriptionActionIcons(final @NotNull Component c, final @NotNull Graphics g, final int x, final int y, final @Nullable String hoveredAction, final boolean isRunning) {
        final int startX = JBUI.scale(16) + x + JBUI.scale(10);

        final Icon navIcon = AllIcons.Nodes.Class;
        final boolean isNavHovered = CardHoverAction.NAVIGATE_TO_TEST_METHOD.name().equals(hoveredAction);
        drawHoverableIcon(c, g, navIcon, startX, y, isNavHovered);

        final int runStartX = startX + navIcon.getIconWidth() + JBUI.scale(8);
        final Icon runIcon = isRunning ? AllIcons.Actions.Suspend : AllIcons.RunConfigurations.TestState.Run;
        final boolean isRunHovered = CardHoverAction.RUN_TEST_CASE.name().equals(hoveredAction);
        drawHoverableIcon(c, g, runIcon, runStartX, y, isRunHovered);
    }

    /**
     * Hands a wheel event to the enclosing scroll pane, so a component that does
     * not scroll itself does not swallow the gesture. Ctrl/Meta is left alone -
     * that is the font zoom, not a scroll.
     */
    public static void forwardWheelToScrollPane(final @NotNull MouseWheelEvent e) {
        if (e.isControlDown() || e.isMetaDown())
            return;

        final JBScrollPane scrollPane = findScrollPane(e.getComponent());

        if (scrollPane != null && e.getComponent() != scrollPane) {
            final MouseWheelEvent clonedEvent = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
            scrollPane.dispatchEvent(clonedEvent);
            e.consume();
        }
    }

    private static @Nullable JBScrollPane findScrollPane(final @Nullable Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JBScrollPane)
                return (JBScrollPane) current;

            current = current.getParent();
        }

        return null;
    }

    private static void drawHoverableIcon(final @NotNull Component c, final @NotNull Graphics g, final @NotNull Icon baseIcon, final int x, final int y, final boolean isHovered) {
        if (isHovered) {
            final Icon scaledIcon = IconUtil.scale(baseIcon, c, 1.5f);
            final int offsetX = (scaledIcon.getIconWidth() - baseIcon.getIconWidth()) / 2;
            final int offsetY = (scaledIcon.getIconHeight() - baseIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(c, g, x - offsetX, y - offsetY);
        } else {
            baseIcon.paintIcon(c, g, x, y);
        }
    }

}