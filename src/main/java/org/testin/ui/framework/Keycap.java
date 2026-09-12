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

package org.testin.ui.framework;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A keystroke, told apart from what it does by weight alone: the key is bold,
 * its meaning is not, and neither has anything drawn around or behind it.
 * <p>
 * <b>The eye should not land here first.</b> A hint strip is the last thing a
 * tester needs and the first thing a drawn key advertises. A filled cap made
 * the brightest things in the window a row of field-colored boxes, ahead of the
 * test case the window exists to show; an outline over the strip's own
 * background was quieter and still a row of boxes. Nothing at all leaves the
 * strip reading as one line of text, which is what a hint is.
 * <p>
 * One owner, because two surfaces show keys and they must show them alike -
 * every framework dialog's status bar, and light mode's verdict buttons, where
 * P, F and B are the primary way a tester works (#13). Drawn in two places, the
 * next adjustment to the spacing or the font would reach one of them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Keycap {

    /**
     * A blank keystroke keeps its spacing to itself. Some status bar items carry
     * no key of their own - a create-dialog field with no letter - and padding
     * around nothing is a gap the tester reads as a missing word.
     */
    public static @NotNull JBLabel of(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(JBUI.CurrentTheme.Label.foreground());
        label.setFont(JBUI.Fonts.smallFont().asBold());

        if (text.isBlank()) return label;

        label.setBorder(JBUI.Borders.empty(0, 5));

        return label;
    }
}
