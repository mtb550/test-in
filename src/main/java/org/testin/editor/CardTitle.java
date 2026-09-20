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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Icons;
import org.testin.model.Automated;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardTitle {
    public static @NotNull ActionIcons descriptionActionIcons(final int titleWidth) {
        final @NotNull Icon icon = Icons.TEST_CASE;
        final int x = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        final int y = JBUI.scale(12);

        return new ActionIcons(
                new Rectangle(x, y, icon.getIconWidth(), icon.getIconHeight()),
                new Rectangle(x + icon.getIconWidth() + JBUI.scale(8), y, icon.getIconWidth(), icon.getIconHeight()));
    }

    public static int titleColumnWidth(final int listWidth) {
        final @NotNull Icon icon = Icons.TEST_CASE;
        final int forIcons = JBUI.scale(10) + icon.getIconWidth() + JBUI.scale(8) + icon.getIconWidth();
        final int column = listWidth - JBUI.scale(16) * 2 - forIcons;

        return column > forIcons ? column : Integer.MAX_VALUE;
    }

    public static void drawDescriptionActionIcons(final @NotNull Project p, final @NotNull Component c, final @NotNull Graphics g, final int titleWidth, final @NotNull String hoveredAction, final @NotNull CardHoverAction runSlot, final @NotNull Automated automation) {
        final @NotNull ActionIcons icons = descriptionActionIcons(titleWidth);

        draw(p, c, g, CardHoverAction.NAVIGATE_TO_TEST_METHOD, automation.getIcon(), icons.navigate(), hoveredAction);
        draw(p, c, g, runSlot, runSlot.getIcon(), icons.run(), hoveredAction);
    }

    // UC-EDITOR-PANEL-047, Rule-CODEGEN-062
    private static void draw(final @NotNull Project p, final @NotNull Component c, final @NotNull Graphics g, final @NotNull CardHoverAction action, final @NotNull Icon icon, final @NotNull Rectangle at, final @NotNull String hoveredAction) {
        final boolean offered = action.isOffered(p);

        drawHoverableIcon(c, g, offered ? icon : IconLoader.getDisabledIcon(icon), at.x, at.y,
                offered && action.name().equals(hoveredAction));
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

    public record ActionIcons(@NotNull Rectangle navigate, @NotNull Rectangle run) {
        public @NotNull Optional<CardHoverAction> at(final int x, final int y, final @NotNull CardHoverAction runSlot) {
            if (grown(navigate).contains(x, y)) return Optional.of(CardHoverAction.NAVIGATE_TO_TEST_METHOD);

            if (grown(run).contains(x, y)) return Optional.of(runSlot);

            return Optional.empty();
        }

        private @NotNull Rectangle grown(final @NotNull Rectangle icon) {
            final int padding = JBUI.scale(4);

            return new Rectangle(icon.x - padding, icon.y - padding,
                    icon.width + padding * 2, icon.height + padding * 2);
        }
    }
}
