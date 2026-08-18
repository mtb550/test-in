package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.Group;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.List;

// Explicit, because java.awt.* above also offers a List and it takes no type
// parameter - the resulting error names the wrong thing and cascades.

public class Shared {

    // ---------------------------------------------------------------- badges
    //
    // One place for badges, look and content together: the constants below are
    // the whole design, the factories say what each badge shows and which colour
    // it takes, Badge is that answer as data, and BadgePill at the bottom paints
    // it. Every badge anywhere in the plugin comes from here - the run card, the
    // test card and the view panel's details - so a change to the look lands on
    // all three at once and none of them can drift. Nothing outside this file
    // builds one.
    //
    // Captioned or bare: a badge is captioned when another badge could be
    // mistaken for it - the test case's priority and the bug's priority declare
    // the same three colours, and severity sits beside both. A badge whose word
    // is its own explanation stays bare.

    private static final int BADGE_RADIUS = 20;

    /**
     * Room around the text, inside the pill.
     */
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

    public static @NotNull Badge createPriorityBadge(final @NotNull TestCaseDto tc) {
        return captioned("Priority", tc.getPriority().getName(), tc.getPriority().getColor());
    }

    /**
     * The test case is not under a test set, so it is not in a run yet.
     */
    public static @NotNull Badge createUnsortedBadge() {
        return new Badge("Unsorted", UNSORTED_COLOR);
    }

    /**
     * The live state of a case while a run is executing - the label and colour
     * are the status's own.
     */
    public static @NotNull Badge createRunStatusBadge(final @NotNull RunStatus.Badge runStatus) {
        return new Badge(runStatus.label(), runStatus.color());
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
    public static void addBadge(final @NotNull List<Badge> badges, final @NotNull String caption,
                                final @NotNull String value, final @NotNull Color color) {
        if (value.isBlank()) return;

        badges.add(captioned(caption, value, color));
    }

    private static @NotNull Badge captioned(final @NotNull String caption, final @NotNull String value,
                                            final @NotNull Color color) {
        return new Badge(caption + ": " + value, color);
    }

    public static @NotNull Badge createGroupBadge(final @NotNull Group group) {
        return new Badge(group.getName(), JBColor.darkGray);
    }

    /**
     * Draws this row of badges in the panel, reusing the pills already in it.
     * <p>
     * The card that owns the panel is one component bound again for every row on
     * the page: the list has no fixed cell height, so a sort, a filter or a page
     * change re-measures all fifty of them before anything is drawn. Throwing the
     * pills away and building new ones each time was several Swing components per
     * row, for a row that usually shows the same two badges as the one before it.
     * <p>
     * A pill past the end of the list is hidden rather than removed - the layout
     * skips an invisible component, and keeping it is what makes the next row
     * free.
     */
    public static void showBadges(final @NotNull JBPanel<?> panel, final @NotNull List<Badge> badges) {
        while (panel.getComponentCount() < badges.size()) {
            panel.add(new BadgePill());
        }

        for (int i = 0; i < panel.getComponentCount(); i++) {
            final BadgePill pill = (BadgePill) panel.getComponent(i);

            if (i < badges.size()) pill.show(badges.get(i));
            else pill.setVisible(false);
        }
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
     * Where the two action icons sit on a card whose title is this wide, in the
     * card's own coordinates.
     * <p>
     * One owner, because two callers need the same answer: the card paints from
     * it and the mouse listener asks it what the pointer is over. They used to
     * work it out separately and had drifted - the painter stepping to the second
     * icon by its width, the hit-test by a rounded literal - so the clickable
     * band no longer covered the icon it belonged to.
     */
    public static @NotNull ActionIcons descriptionActionIcons(final int titleWidth) {
        final Icon icon = AllIcons.Nodes.Class;
        final int x = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        final int y = JBUI.scale(12);

        return new ActionIcons(
                new Rectangle(x, y, icon.getIconWidth(), icon.getIconHeight()),
                new Rectangle(x + icon.getIconWidth() + JBUI.scale(8), y, icon.getIconWidth(), icon.getIconHeight()));
    }

    public static void drawDescriptionActionIcons(final @NotNull Component c, final @NotNull Graphics g, final int titleWidth, final @Nullable String hoveredAction, final boolean isRunning) {
        final ActionIcons icons = descriptionActionIcons(titleWidth);

        if (CardHoverAction.NAVIGATE_TO_TEST_METHOD.isOffered()) {
            drawHoverableIcon(c, g, AllIcons.Nodes.Class, icons.navigate().x, icons.navigate().y,
                    CardHoverAction.NAVIGATE_TO_TEST_METHOD.name().equals(hoveredAction));
        }

        if (CardHoverAction.RUN_TEST_CASE.isOffered()) {
            drawHoverableIcon(c, g, isRunning ? AllIcons.Actions.Suspend : AllIcons.RunConfigurations.TestState.Run,
                    icons.run().x, icons.run().y,
                    CardHoverAction.RUN_TEST_CASE.name().equals(hoveredAction));
        }
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

    /**
     * The two icons drawn after a card's title, and the question the mouse asks
     * of them.
     */
    public record ActionIcons(@NotNull Rectangle navigate, @NotNull Rectangle run) {

        /**
         * Which action the pointer is over, or none. The bands are grown a little
         * past the icons: a 16-pixel target is hard to hold, and being generous
         * here is safe while nothing else on the title line is clickable.
         */
        public @Nullable CardHoverAction at(final int x, final int y) {
            // An action this IDE does not offer is not drawn, so nothing is over
            // it either - the band belongs to the icon, and there is no icon.
            if (CardHoverAction.NAVIGATE_TO_TEST_METHOD.isOffered() && grown(navigate).contains(x, y))
                return CardHoverAction.NAVIGATE_TO_TEST_METHOD;

            if (CardHoverAction.RUN_TEST_CASE.isOffered() && grown(run).contains(x, y))
                return CardHoverAction.RUN_TEST_CASE;

            return null;
        }

        private @NotNull Rectangle grown(final @NotNull Rectangle icon) {
            final int padding = JBUI.scale(4);

            return new Rectangle(icon.x - padding, icon.y - padding,
                    icon.width + padding * 2, icon.height + padding * 2);
        }
    }

    /**
     * What a badge shows: a word, and the colour it is drawn in. Data rather
     * than a component, so an attribute deciding what a row says never builds
     * one - it describes it, the way it already hands its detail row over as
     * text, and the panel that draws them owns the components.
     */
    public record Badge(@NotNull String text, @NotNull Color color) {
    }

    /**
     * The pill: a rounded label that draws its own background and picks its own
     * text colour. Private, because a badge is asked for by name above and never
     * assembled by a caller - that is what keeps the look in one place.
     */
    private static final class BadgePill extends JBLabel {

        private BadgePill() {
            setOpaque(false);
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H));
        }

        /**
         * Shows this badge. The font is derived here rather than in the
         * constructor because a pill outlives the row it was first built for,
         * and the base size follows the tester's zoom.
         */
        private void show(final @NotNull Badge badge) {
            setText(badge.text());
            setBackground(badge.color());

            final float badgeSize = Math.max(8.0f, FontSync.getBaseFontSize() - 2.0f);
            setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD, badgeSize));

            setVisible(true);
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

}