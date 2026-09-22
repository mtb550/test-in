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
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Font;
import java.util.Locale;
import org.testin.util.Fonts;

// Rule-INTERNAL-087
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Caption {
    public static @NotNull JBLabel of(final @NotNull String text, final @NotNull Font font) {
        final @NotNull JBLabel label = new JBLabel(text.toUpperCase(Locale.ROOT));

        label.setFont(font);
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        return label;
    }

    public static @NotNull BorderLayoutPanel above(final @NotNull String caption, final @NotNull JComponent value) {
        final @NotNull BorderLayoutPanel panel = JBUI.Panels.simplePanel(0, 2).addToCenter(value).withBorder(JBUI.Borders.emptyTop(8)).andTransparent();
        if (!caption.isEmpty())
            panel.addToTop(of(caption, Fonts.caption()).withBorder(JBUI.Borders.emptyLeft(12)));
        return panel;
    }
}
