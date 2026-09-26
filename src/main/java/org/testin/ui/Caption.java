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

import com.intellij.ui.JBColor;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.SeparatorOrientation;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Locale;
import java.util.Optional;

// Rule-INTERNAL-087
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Caption {
    private static final int GAP = 8;

    public static @NotNull JBLabel of(final @NotNull String text, final @NotNull Font font) {
        final @NotNull JBLabel label = new JBLabel(text.toUpperCase(Locale.ROOT));

        label.setFont(font);
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        return label;
    }

    // Rule-INTERNAL-099
    public static @NotNull JBPanel<?> header(final @NotNull JComponent title, final @NotNull Optional<JComponent> trailing) {
        final @NotNull JBPanel<?> row = new JBPanel<>(new GridBagLayout());
        row.setOpaque(false);

        final @NotNull GridBagConstraints placed = new GridBagConstraints();
        placed.gridy = 0;
        row.add(title, placed);

        placed.weightx = 1;
        placed.fill = GridBagConstraints.HORIZONTAL;
        placed.insets = JBUI.insets(0, GAP, 0, GAP);
        row.add(new SeparatorComponent(JBColor.border(), SeparatorOrientation.HORIZONTAL), placed);

        placed.weightx = 0;
        placed.fill = GridBagConstraints.NONE;
        placed.insets = JBUI.emptyInsets();
        trailing.ifPresent(component -> row.add(component, placed));

        return row;
    }

    public static @NotNull BorderLayoutPanel above(final @NotNull String caption, final @NotNull JComponent value) {
        final @NotNull BorderLayoutPanel panel = JBUI.Panels.simplePanel(0, 2).addToCenter(value).withBorder(JBUI.Borders.emptyTop(8)).andTransparent();
        if (!caption.isEmpty()) panel.addToTop(of(caption, Fonts.caption()));
        return panel;
    }
}
