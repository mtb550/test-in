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

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * Rule-INTERNAL-087.
 * <p>
 * What a caption looks like wherever Testin names a value: JetBrains Mono, a
 * family of its own beside the UI font the values are in, two points below the
 * size of the text around it, in capitals, in the muted caption gray (#328).
 * <p>
 * One owner, because the plugin had three: the details panel set its captions
 * bold at the editor size, the dialogs in the platform's small font, and light
 * mode in small capitals. Changed here, every caption follows.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Caption {

    /**
     * Bundled with the IDE: the JetBrains Runtime ships and registers it
     * ({@code jbr/lib/fonts/JetBrainsMono-Regular.ttf}), so it is there on every
     * OS whatever the tester's settings, and Testin ships no font of its own.
     */
    private static final @NotNull String FAMILY = "JetBrains Mono";

    private static final int SMALLER = 2;

    /**
     * A caption for the value it names.
     *
     * @param baseSize the size of the text around it: the editor font size where
     *                 a test case is read, the UI font size in a dialog. Already in
     *                 screen terms, so the caption is not scaled a second time
     */
    public static @NotNull JBLabel of(final @NotNull String text, final float baseSize) {
        final @NotNull JBLabel label = new JBLabel(text.toUpperCase(Locale.ROOT));

        // Wrapped with the platform's fallback: JetBrains Mono has no
        // Devanagari, and a plain Java font draws a glyph it lacks as an empty
        // box, so a Hindi caption would read as a row of them.
        label.setFont(UIUtil.getFontWithFallback(new Font(FAMILY, Font.PLAIN, 1).deriveFont(Math.max(FontSync.FLOOR, baseSize - SMALLER))));
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        return label;
    }

    /**
     * A dialog's value with its caption above it, at the dialog's own size:
     * the one shape every captioned component of the dialog framework takes,
     * so their gaps cannot drift apart. No caption, no line for one - the
     * value keeps the same space above it.
     */
    public static @NotNull BorderLayoutPanel above(final @NotNull String caption, final @NotNull JComponent value) {
        final @NotNull BorderLayoutPanel panel = JBUI.Panels.simplePanel(0, JBUI.scale(2)).addToCenter(value).withBorder(JBUI.Borders.emptyTop(8)).andTransparent();
        if (!caption.isEmpty()) panel.addToTop(of(caption, JBUI.Fonts.label().getSize2D()));
        return panel;
    }
}
