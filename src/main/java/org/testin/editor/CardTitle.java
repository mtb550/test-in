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

package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Automated;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * The title line of a card: how wide the words may run, and where the two
 * action icons after them sit.
 * <p>
 * One owner, because the card paints from these numbers and the mouse listener
 * hit-tests with them. They used to be worked out separately and had drifted -
 * the painter stepping to the second icon by its width, the hit-test by a
 * rounded literal - so the clickable band no longer covered the icon it
 * belonged to.
 * <p>
 * Split out of editor/Shared, which held this beside every badge in the plugin
 * and a wheel forwarder (#113).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardTitle {
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
        final @NotNull Icon icon = AllIcons.Nodes.Class;
        final int x = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        final int y = JBUI.scale(12);

        return new ActionIcons(
                new Rectangle(x, y, icon.getIconWidth(), icon.getIconHeight()),
                new Rectangle(x + icon.getIconWidth() + JBUI.scale(8), y, icon.getIconWidth(), icon.getIconHeight()));
    }

    /**
     * How far a card title may run before it wraps, in a list this wide - and so
     * also the widest {@code titleWidth} {@link #descriptionActionIcons} is ever
     * given, since past it the icons would be drawn off the card.
     * <p>
     * The card's own insets on each side, and then the room the two icons need
     * after the text. Both numbers are read off the icons and the same scaled
     * inset the method above starts from, so the column ends exactly where the
     * icons must still fit.
     * <p>
     * Here for the same reason the icon positions are: the card wraps its title
     * at this width and caps the width it paints from, and the mouse listener
     * caps the width it hit-tests with. A second copy of the arithmetic is a
     * clickable band that stops covering the icon it belongs to.
     */
    public static int titleColumnWidth(final int listWidth) {
        final @NotNull Icon icon = AllIcons.Nodes.Class;
        final int forIcons = JBUI.scale(10) + icon.getIconWidth() + JBUI.scale(8) + icon.getIconWidth();
        final int column = listWidth - JBUI.scale(16) * 2 - forIcons;

        // No column is not a narrow column. A list that has not been laid out yet
        // reports zero width, and a card asked to wrap inside nothing draws one
        // character per line. So until there is more room for the words than the
        // icons themselves take, the title runs as far as it likes - which is
        // what every card did before it could wrap at all.
        return column > forIcons ? column : Integer.MAX_VALUE;
    }

    /**
     * Draws the card's action icons: the navigate button, and whichever of the
     * run and stop buttons this card's state offers.
     */
    public static void drawDescriptionActionIcons(final @NotNull Component c, final @NotNull Graphics g, final int titleWidth, final @NotNull String hoveredAction, final @NotNull CardHoverAction runSlot, final @NotNull Automated automation) {
        final @NotNull ActionIcons icons = descriptionActionIcons(titleWidth);

        // Both under the pointer, as they always were. Drawing the navigate icon
        // on every card was tried and taken back: three shapes down every row is
        // a lot of chrome for a fact most cards share, and the filter answers
        // "which of these are automated" better than eighty small icons do.
        drawIfOffered(c, g, CardHoverAction.NAVIGATE_TO_TEST_METHOD, automation.getIcon(), icons.navigate(), hoveredAction);
        drawIfOffered(c, g, runSlot, runSlot.getIcon(), icons.run(), hoveredAction);
    }

    /**
     * One button, drawn where it sits, and left out entirely in an IDE that
     * cannot act on it.
     */
    private static void drawIfOffered(final @NotNull Component c, final @NotNull Graphics g, final @NotNull CardHoverAction action, final @NotNull Icon icon, final @NotNull Rectangle at, final @NotNull String hoveredAction) {
        if (!action.isOffered()) return;

        drawHoverableIcon(c, g, icon, at.x, at.y, action.name().equals(hoveredAction));
    }
    private static void drawHoverableIcon(final @NotNull Component c, final @NotNull Graphics g, final @NotNull Icon baseIcon, final int x, final int y, final boolean isHovered) {
        if (isHovered) {
            final @NotNull Icon scaledIcon = IconUtil.scale(baseIcon, c, 1.5f);
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
         * Which action the pointer is over, or nothing at all. The bands are
         * grown a little past the icons: a 16-pixel target is hard to hold, and
         * being generous here is safe while nothing else on the title line is
         * clickable.
         */
        public @NotNull Optional<CardHoverAction> at(final int x, final int y, final @NotNull CardHoverAction runSlot) {
            // An action this IDE does not offer is not drawn, so nothing is over
            // it either - the band belongs to the icon, and there is no icon.
            if (CardHoverAction.NAVIGATE_TO_TEST_METHOD.isOffered() && grown(navigate).contains(x, y))
                return Optional.of(CardHoverAction.NAVIGATE_TO_TEST_METHOD);

            if (runSlot.isOffered() && grown(run).contains(x, y))
                return Optional.of(runSlot);

            return Optional.empty();
        }

        private @NotNull Rectangle grown(final @NotNull Rectangle icon) {
            final int padding = JBUI.scale(4);

            return new Rectangle(icon.x - padding, icon.y - padding,
                    icon.width + padding * 2, icon.height + padding * 2);
        }
    }
}
