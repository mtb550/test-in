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
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The icons Testin makes for itself: a stock one grayed or enlarged, a colored
 * dot, and a test case field's letter in a frame.
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
     * The color a field's letter and frame are drawn in: the platform's own
     * icon gray, one value in every theme, as Muteb chose. Named here so a
     * caller says which, and a change to it is made once.
     */
    public static final @NotNull Color GRAY = new Color(0x6C707E);

    /**
     * UC-EDITOR-PANEL-005.
     * <p>
     * A test case field, wherever one is offered - the create and update forms
     * and the update menu: the letter of the key that opens it, in a rounded
     * frame, the frame and the letter in the one color given (#328).
     * <p>
     * Drawn here rather than put together from the platform's {@code TextIcon}
     * over a frame file. That pair left the letter a pixel above and to the
     * left of the middle - {@code TextIcon} measures its box one way and draws
     * in it another - and tinting the file kept its brightness, so a black shape
     * stayed black whatever the color (measured, #328).
     * <p>
     * <b>The letter is drawn as a shape, like the frame</b>, and centered on
     * that shape. Drawn as text, the screen's font hinting narrowed it and
     * snapped it to whole pixels, so at 125% the letter on screen was not the
     * one measured and sat a pixel left of the middle. A shape is painted
     * exactly where it is measured, at every scale.
     */
    public static @NotNull Icon fieldLetter(final @NotNull String letter, final @NotNull Color color) {
        final @NotNull Font font = JBUI.Fonts.label(9f).asBold();
        return new Icon() {
            @Override
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
                final @NotNull Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);

                    // Half the one-pixel line inside the edge, so the line is sharp.
                    final float edge = JBUIScale.scale(1.5f);
                    g2.setStroke(new BasicStroke(JBUIScale.scale(1f)));
                    g2.draw(new RoundRectangle2D.Float(x + edge, y + edge, getIconWidth() - 2 * edge, getIconHeight() - 2 * edge, JBUIScale.scale(5f), JBUIScale.scale(5f)));

                    final @NotNull Shape glyph = new TextLayout(letter, font, g2.getFontRenderContext()).getOutline(null);
                    final @NotNull Rectangle2D ink = glyph.getBounds2D();
                    g2.translate(x + (getIconWidth() - ink.getWidth()) / 2 - ink.getX(), y + (getIconHeight() - ink.getHeight()) / 2 - ink.getY());
                    g2.fill(glyph);
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