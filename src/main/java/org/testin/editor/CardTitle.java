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

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Automated;
import org.testin.util.Icons;

import javax.swing.Icon;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardTitle {
    private static final float TITLE_FONT_DELTA = 3.0f;

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-235
    public static @NotNull ActionIcons descriptionActionIcons(final int titleWidth, final @NotNull List<CardHoverAction.Offered> buttons) {
        final @NotNull Icon slot = Icons.TEST_CASE;
        final int first = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        final int step = slot.getIconWidth() + JBUI.scale(8);
        final int y = JBUI.scale(12);

        return new ActionIcons(IntStream.range(0, buttons.size())
                .mapToObj(i -> new Slot(buttons.get(i), new Rectangle(first + i * step, y, slot.getIconWidth(), slot.getIconHeight())))
                .toList());
    }

    // Rule-EDITOR-PANEL-003
    public static @NotNull Font titleFont(final @NotNull JList<?> list) {
        return list.getFont().deriveFont(Font.BOLD, list.getFont().getSize2D() + TITLE_FONT_DELTA);
    }

    // Rule-EDITOR-PANEL-003, Rule-EDITOR-PANEL-235
    public static int titleWidth(final @NotNull JList<?> list, final @NotNull String title, final int count) {
        return Math.min(list.getFontMetrics(titleFont(list)).stringWidth(title), titleColumnWidth(list.getWidth(), count));
    }

    public static int titleColumnWidth(final int listWidth, final int count) {
        final @NotNull Icon slot = Icons.TEST_CASE;
        final int forIcons = JBUI.scale(10) + count * slot.getIconWidth() + Math.max(0, count - 1) * JBUI.scale(8);
        final int column = listWidth - JBUI.scale(16) * 2 - forIcons;

        return column > forIcons ? column : Integer.MAX_VALUE;
    }

    public static void drawDescriptionActionIcons(final @NotNull Component c, final @NotNull Graphics g, final int titleWidth, final @NotNull String hoveredAction, final @NotNull List<CardHoverAction.Offered> buttons, final @NotNull Automated automation) {
        descriptionActionIcons(titleWidth, buttons).slots()
                .forEach(slot -> draw(c, g, slot.button(), slot.button().action().iconOn(automation), slot.at(), hoveredAction));
    }

    // UC-EDITOR-PANEL-047, Rule-CODEGEN-062, Rule-EDITOR-PANEL-235
    private static void draw(final @NotNull Component c, final @NotNull Graphics g, final @NotNull CardHoverAction.Offered button, final @NotNull Icon icon, final @NotNull Rectangle at, final @NotNull String hoveredAction) {
        final @NotNull Icon shown = button.works() ? icon : IconLoader.getDisabledIcon(icon);

        drawHoverableIcon(c, g, shown, at.x + (at.width - shown.getIconWidth()) / 2, at.y + (at.height - shown.getIconHeight()) / 2,
                button.works() && button.action().name().equals(hoveredAction));
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

    public record Slot(@NotNull CardHoverAction.Offered button, @NotNull Rectangle at) {
    }

    public record ActionIcons(@NotNull List<Slot> slots) {
        private static @NotNull Rectangle grown(final @NotNull Rectangle icon) {
            final int padding = JBUI.scale(4);

            return new Rectangle(icon.x - padding, icon.y - padding,
                    icon.width + padding * 2, icon.height + padding * 2);
        }

        public @NotNull Optional<CardHoverAction.Offered> at(final int x, final int y) {
            return slots.stream().filter(slot -> grown(slot.at()).contains(x, y)).map(Slot::button).findFirst();
        }
    }
}
