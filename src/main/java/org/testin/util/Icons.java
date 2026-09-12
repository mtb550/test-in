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

package org.testin.util;

import com.intellij.icons.AllIcons;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * The two icons Testin makes for itself: a stock one enlarged, and a colored
 * dot.
 * <p>
 * Was {@code IconManager}, which managed nothing and is also the name of
 * {@code com.intellij.ui.IconManager} in the platform - the same collision
 * {@code EditorUtil} had, and the reason CLAUDE.md asks for a name that says
 * what the class does (#291).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Icons {

    /**
     * UC-EDITOR-PANEL-001, UC-INTERNAL-001.
     * <p>
     * What a test case looks like, wherever one is drawn: the card, the card's
     * hover action, and a row in the search.
     * <p>
     * Here rather than at each of the three, because it is one answer to one
     * question and they were three copies of it - a change had to be made three
     * times and noticed three times.
     * <p>
     * <b>Not the class icon.</b> It was, and that picture means a Java class -
     * which is what {@link org.testin.model.Automated} still draws for the
     * generated method, on the same card. Two things on one card cannot be the
     * same picture, and of the two it is the Java class that has the better
     * claim to it.
     * <p>
     * <b>Drawn gray</b>, like everything else - see {@link #gray}.
     */
    public static final @NotNull Icon TEST_CASE = gray(AllIcons.Nodes.Type);

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-077.
     * <p>
     * The same icon with the color taken out of it.
     * <p>
     * <b>Testin's surfaces are gray.</b> A plugin draws stock platform icons,
     * and a handful of them ship colored - {@code Nodes.Type} is a
     * {@code #3574F0} ring and stem over a pale blue fill. One colored glyph in
     * a list of gray ones is the loudest thing on the row, and the row is for
     * reading what it says rather than for looking at its icon.
     * <p>
     * Desaturated through the platform rather than by keeping a second copy of
     * the file, so it follows the icon if JetBrains redraws it.
     * <p>
     * <b>Not for a color that means something.</b> An error is red and a passed
     * test is green because the color is the fact, not decoration - those are
     * drawn as they are. This is for an icon that names a thing.
     */
    public static @NotNull Icon gray(final @NotNull Icon icon) {
        return IconUtil.desaturate(icon);
    }

    private static final float SCALE_FACTOR = 1.3f;
    private static final int DOT_SIZE = 10;

    public static @NotNull Icon zoomStandardIcon(final @NotNull Icon icon, final @NotNull Component contextComponent) {
        return IconUtil.scale(icon, contextComponent, SCALE_FACTOR);
    }

    /**
     * The colored dot the priority and group rows are marked with, centered in a
     * standard 16px icon.
     */
    public static @NotNull Icon dot(final @NotNull Color color) {
        return new Icon() {
            @Override
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
                final @NotNull Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);

                    final int size = JBUI.scale(DOT_SIZE);
                    final int totalWidth = getIconWidth();
                    final int totalHeight = getIconHeight();
                    final int centeredX = x + (totalWidth - size) / 2;
                    final int centeredY = y + (totalHeight - size) / 2;
                    g2.fillOval(centeredX, centeredY, size, size);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return JBUI.scale(16);
            }

            @Override
            public int getIconHeight() {
                return JBUI.scale(16);
            }
        };
    }
}