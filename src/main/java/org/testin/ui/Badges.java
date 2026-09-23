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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Fonts;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Badges {
    private static final int BADGE_RADIUS = 20;

    private static final int TAG_NOTCH = 7;

    private static final int BADGE_ICON_GAP = 4;

    private static final @NotNull String PAIR_JOIN = " / ";

    private static final int BADGE_PAD_V = 2;
    private static final int BADGE_PAD_H = 10;

    private static final @NotNull Color TEXT_ON_LIGHT = Gray._30;

    private static final @NotNull Color GROUP_COLOR = JBColor.darkGray;
    private static final @NotNull Icon BUG_MARK = IconUtil.colorize(IconUtil.resizeSquared(AllIcons.Toolwindows.ToolWindowDebugger, 20), Gray._0);

    public static void addPriorityBadge(final @NotNull List<Badge> badges, final @NotNull TestCaseDto tc) {
        if (tc.getPriority() == Priority.LOW) return;

        badges.add(new Pill(tc.getPriority().getLabel(), tc.getPriority().getColor()));
    }

    // UC-EDITOR-PANEL-001
    public static @NotNull List<Badge> testCaseBadges(final @NotNull TestCaseDto tc) {
        final @NotNull List<Badge> badges = new ArrayList<>();
        addPriorityBadge(badges, tc);

        for (final String group : tc.getGroup()) {
            badges.add(createGroupBadge(group));
        }

        return badges;
    }

    public static @NotNull Badge createRunStatusBadge(final @NotNull RunStatus.Badge runStatus) {
        return new Pill(runStatus.label(), runStatus.color());
    }

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

    public static @NotNull Badge createGroupBadge(final @NotNull String group) {
        return new Tag(group, GROUP_COLOR);
    }

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

    static boolean isLight(final @NotNull Color bg) {
        return 0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue() > 140;
    }

    public sealed interface Badge permits Pill, Tag, Bug {
    }

    public record Pill(@NotNull String text, @NotNull Color color) implements Badge {
    }

    public record Tag(@NotNull String text, @NotNull Color color) implements Badge {
    }

    public record Bug(@NotNull String text, @NotNull Color color) implements Badge {
    }

    private static final class BadgePill extends JBLabel {
        private @NotNull Badge badge = new Pill("", JBColor.GRAY);

        private BadgePill() {
            setOpaque(false);
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H));
            setIconTextGap(JBUI.scale(BADGE_ICON_GAP));
        }

        private void show(final @NotNull Badge badge) {
            this.badge = badge;

            switch (badge) {
                case Pill pill -> lay(pill.text(), pill.color(), BADGE_PAD_H, EmptyIcon.ICON_0);
                case Bug bug -> lay(bug.text(), bug.color(), BADGE_PAD_H, BUG_MARK);
                case Tag tag -> lay(tag.text(), tag.color(), BADGE_PAD_H + TAG_NOTCH, EmptyIcon.ICON_0);
            }

            setFont(Fonts.badge());

            setVisible(true);
        }

        private void lay(final @NotNull String text, final @NotNull Color fill, final int rightPad, final @NotNull Icon icon) {
            setText(text);
            setBackground(fill);
            setIcon(icon);
            setBorder(JBUI.Borders.empty(BADGE_PAD_V, BADGE_PAD_H, BADGE_PAD_V, rightPad));
        }

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
