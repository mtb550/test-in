/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every badge in the plugin: what one shows, and how one is drawn.
 * <p>
 * Split out of editor/Shared, which held this, the card's title geometry and a
 * wheel forwarder under one name three packages imported (#113). Here rather
 * than in the editor because the editor is not the only surface that shows a
 * badge - the view panel's details and light mode do too, and a badge that
 * lived with one of them would be a look the other two reach across for.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Badges {
    // ---------------------------------------------------------------- badges
    //
    // One place for badges, look and content together: the constants below are
    // the whole design, the factories say what each badge shows and which color
    // it takes, Badge is that answer as data, and BadgePill at the bottom paints
    // it. Every badge anywhere in the plugin comes from here - the run card, the
    // test card and the view panel's details - so a change to the look lands on
    // all three at once and none of them can drift. Nothing outside this file
    // builds one.
    //
    // Four kinds of fact, told apart without a caption. A caption used to do
    // that work - the
    // test case's priority and the bug's priority declare the same three colors,
    // so a card read "Priority: High" beside "Bug Priority: High" and half of
    // each pill was spent saying which field it was. The shape says it now, and
    // the words are back to being the value (#89):
    //
    //   priority   ( P1 )              a filled, rounded pill
    //   group      [ Regression <      a ribbon, notched at its right end
    //   bug        ( * Major / High )  one pill, marked with the debugger's bug
    //
    // The bug badge is a pill and not a shape of its own on purpose: two colored
    // halves meant clipping, font metrics and two hand-drawn strings, which is
    // twenty lines of painting that nothing tests and a font change breaks. Two
    // words and a slash tell it from a priority, and cost nothing.
    //
    // The run status badge keeps the plain pill: its word is its own
    // explanation and it collides with nothing.

    private static final int BADGE_RADIUS = 20;

    /**
     * How deep the notch is cut into a tag's right end, and the room left for it
     * so the last letter does not sit in the cut.
     */
    private static final int TAG_NOTCH = 7;

    /**
     * Between a badge's mark and its first word.
     */
    private static final int BADGE_ICON_GAP = 4;

    /**
     * What joins the two halves of a bug badge into one word to read.
     */
    private static final @NotNull String PAIR_JOIN = " / ";

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
     * One color for every group, because it is a property of the tag design
     * rather than of any group: eight of these can sit on one row, and eight
     * hues there is a wall rather than a row. What tells them apart is the word,
     * which is the whole reason a tag is a tag.
     */
    private static final @NotNull Color GROUP_COLOR = JBColor.darkGray;

    /**
     * The case's priority, and nothing at all when it is Low.
     * <p>
     * Low is what a case is unless somebody said otherwise, so a Low pill is a
     * badge on almost every row saying what the absence of a badge already says.
     * Dropping it leaves the row carrying only the priorities that were a
     * decision (#89).
     * <p>
     * Added rather than returned, so the rule lives here and every caller stays
     * unconditional - the same reason {@link #addBugBadge} is shaped this way.
     */
    public static void addPriorityBadge(final @NotNull List<Badge> badges, final @NotNull TestCaseDto tc) {
        if (tc.getPriority() == Priority.LOW) return;

        badges.add(new Pill(tc.getPriority().getLabel(), tc.getPriority().getColor()));
    }

    /**
     * UC-EDITOR-PANEL-001.
     * <p>
     * The badges a test case carries on its own: its priority, then one per
     * group. Not its run status - that belongs to a run rather than to the case,
     * so the surfaces that want it add it after these.
     */
    public static @NotNull List<Badge> caseBadges(final @NotNull TestCaseDto tc) {
        final @NotNull List<Badge> badges = new ArrayList<>();
        addPriorityBadge(badges, tc);

        for (final String group : tc.getGroup()) {
            badges.add(createGroupBadge(group));
        }

        return badges;
    }

    /**
     * The live state of a case while a run is executing - the label and color
     * are the status's own.
     */
    public static @NotNull Badge createRunStatusBadge(final @NotNull RunStatus.Badge runStatus) {
        return new Pill(runStatus.label(), runStatus.color());
    }

    /**
     * How badly the case failed and how urgently the bug wants fixing, as one
     * object.
     * <p>
     * They are two halves of one fact and never appear apart, so they are one
     * badge rather than two pills a tester has to pair up by eye. Severity's
     * color, because severity is how bad it is and its palette is the traffic
     * light built to be read at a glance.
     * <p>
     * Two words and a slash are also what tells this from a priority pill, which
     * is the collision the old captions existed to prevent. A second color for
     * the second half would say it better and costs a shape of its own to paint,
     * which is more painting code than the difference is worth.
     * <p>
     * One half each, and one badge either way. Severity and bug priority are
     * two ticks on the Details toolbar because they are also two grid columns,
     * so each has to be able to leave on its own - a tester who unticks Bug
     * Priority wants the priority gone, not the whole badge. So each half adds
     * itself, and the second finds the first and joins it.
     * <p>
     * The color is the first half's, which is severity's when both are shown and
     * the survivor's when only one is. Nothing at all for a half with no value:
     * a case that never failed has neither.
     */
    public static void addBugBadge(final @NotNull List<Badge> badges, final @NotNull String value, final @NotNull Color color) {
        if (value.isBlank()) return;

        for (int i = 0; i < badges.size(); i++) {
            if (badges.get(i) instanceof Bug(String text, Color color1)) {
                badges.set(i, new Bug(text + PAIR_JOIN + value, color1));
                return;
            }
        }

        badges.add(new Bug(value, color));
    }

    /**
     * A group, as a tag: the ribbon shape is what says this is a label the
     * tester put on the case rather than a state the case is in.
     */
    public static @NotNull Badge createGroupBadge(final @NotNull String group) {
        return new Tag(group, GROUP_COLOR);
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
            final @NotNull BadgePill pill = (BadgePill) panel.getComponent(i);

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
     * What a badge shows: a word, and the color it is drawn in. Data rather
     * than a component, so an attribute deciding what a row says never builds
     * one - it describes it, the way it already hands its detail row over as
     * text, and the panel that draws them owns the components.
     */
    public sealed interface Badge permits Pill, Tag, Bug {
    }

    /**
     * A rounded, filled pill - the shape a badge has had all along. A priority
     * and a run status.
     */
    public record Pill(@NotNull String text, @NotNull Color color) implements Badge {
    }

    /**
     * A ribbon, notched at its right end. A group.
     */
    public record Tag(@NotNull String text, @NotNull Color color) implements Badge {
    }

    /**
     * The bug's badge: a pill, marked with the platform's own debugger bug.
     * <p>
     * Its own type for two reasons. The second half finds the first by it and
     * joins it rather than sitting beside it, and it is what carries the mark -
     * a priority pill and a bug pill are otherwise the same thing to look at,
     * and the mark is what says which one this is without a caption (#89).
     */
    public record Bug(@NotNull String text, @NotNull Color color) implements Badge {
    }

    /**
     * The mark on a bug badge: the platform's own debugger bug, at 20 pixels
     * and solid black.
     * <p>
     * The platform's, so a tester reads it as the bug it means everywhere else
     * in the IDE rather than as a shape this plugin invented. Twenty because it
     * ships at 16 and the badge's text is smaller than the label font, so at its
     * own size it disappears - and because 20 is a size this icon really has:
     * it is what the New UI's tool window stripe draws.
     * <p>
     * Black in both themes, and {@link Gray} rather than {@link JBColor} for the
     * same reason {@link #TEXT_ON_LIGHT} is: what it sits on is the badge's own
     * fill, chosen by the severity being shown, so it does not follow the editor
     * theme and neither should this. Untinted it ships in the platform's
     * monochrome grey, which is what a Blocker's red would have swallowed.
     * <p>
     * Built once. It is the same mark on every row, and a colorize per bind is a
     * cost the pooled pills exist to avoid.
     */
    private static final @NotNull Icon BUG_MARK = IconUtil.colorize(IconUtil.resizeSquared(AllIcons.Toolwindows.ToolWindowDebugger, 20), Gray._0);

    /**
     * The pill: a rounded label that draws its own background and picks its own
     * text color. Private, because a badge is asked for by name above and never
     * assembled by a caller - that is what keeps the look in one place.
     */
    private static final class BadgePill extends JBLabel {

        /**
         * What this pill is drawing. A pill outlives the row it was built for,
         * so the shape is read at paint time rather than fixed at construction -
         * the same reason the font is. An empty one until it is shown, so no
         * reader has to ask whether it has been.
         */
        private @NotNull Badge badge = new Pill("", JBColor.GRAY);

        private BadgePill() {
            setOpaque(false);
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H));
            // Set once: a pill with no mark has a zero-width icon, so the gap
            // costs it nothing.
            setIconTextGap(JBUI.scale(BADGE_ICON_GAP));
        }

        /**
         * Shows this badge. The font is derived here rather than in the
         * constructor because a pill outlives the row it was first built for,
         * and the base size follows the tester's zoom.
         */
        private void show(final @NotNull Badge badge) {
            this.badge = badge;

            switch (badge) {
                case Pill pill -> lay(pill.text(), pill.color(), BADGE_PAD_H, EmptyIcon.ICON_0);
                case Bug bug -> lay(bug.text(), bug.color(), BADGE_PAD_H, BUG_MARK);
                // Room on the right for the notch, so the last letter is not cut.
                case Tag tag -> lay(tag.text(), tag.color(), BADGE_PAD_H + TAG_NOTCH, EmptyIcon.ICON_0);
            }

            final float badgeSize = Math.max(8.0f, FontSync.getBaseFontSize() - 2.0f);
            setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD, badgeSize));

            setVisible(true);
        }

        /**
         * {@code EmptyIcon.ICON_0} for a badge with no mark, rather than a null -
         * it is the platform's own way of saying "no icon", it takes no room,
         * and it keeps this method's contract the same for all three.
         */
        private void lay(final @NotNull String text, final @NotNull Color fill, final int rightPad, final @NotNull Icon icon) {
            setText(text);
            setBackground(fill);
            setIcon(icon);
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H, BADGE_PAD_V, rightPad));
        }

        /**
         * Derived rather than set, because the badge is the only thing that knows
         * what it is drawn on. White was hard-coded, which is fine on the deep
         * reds and grays the cards started with and unreadable the moment a value
         * takes a light color - a yellow severity pill with white text says
         * nothing. Computed per call rather than in the constructor because a
         * JBColor resolves to a different value in the dark theme, and the answer
         * has to follow it.
         */
        @Override
        public Color getForeground() {
            return Optional.ofNullable(getBackground())
                    .map(bg -> isLight(bg) ? TEXT_ON_LIGHT : JBColor.WHITE)
                    .orElseGet(super::getForeground);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            switch (badge) {
                case Pill pill -> fillPill(g2, pill.color());
                case Bug bug -> fillPill(g2, bug.color());
                case Tag tag -> fillTag(g2, tag.color());
            }

            g2.dispose();

            super.paintComponent(g);
        }

        private void fillPill(final @NotNull Graphics2D g2, final @NotNull Color fill) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), BADGE_RADIUS, BADGE_RADIUS);
        }

        /**
         * The ribbon: square on the left, and a V cut into the right end.
         */
        private void fillTag(final @NotNull Graphics2D g2, final @NotNull Color fill) {
            final int w = getWidth();
            final int h = getHeight();
            final int notch = JBUI.scale(TAG_NOTCH);

            final int @NotNull [] x = {0, w, w - notch, w, 0};
            final int @NotNull [] y = {0, 0, h / 2, h, h};

            g2.setColor(fill);
            g2.fillPolygon(x, y, x.length);
        }
    }
}
