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
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * The framework's caption column: platform hint style (small, gray), one width
 * across a dialog so captioned rows align across components.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Captions {

    /**
     * Marks a caption panel, holding the width its own text needs.
     */
    private static final @NotNull String NEEDS = "testin.caption.needs";

    static @NotNull JBPanel<?> panel(final @NotNull String caption) {
        final @NotNull JBPanel<?> captionPanel = new JBPanel<>(new GridBagLayout());
        captionPanel.setOpaque(false);

        final @NotNull JBLabel label = new JBLabel(caption);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        captionPanel.add(label);

        captionPanel.putClientProperty(NEEDS, captionPanel.getPreferredSize().width + JBUI.scale(12));
        captionPanel.setPreferredSize(new Dimension(JBUI.scale(96), captionPanel.getPreferredSize().height));
        return captionPanel;
    }

    /**
     * Rule-INTERNAL-086.
     * <p>
     * Every caption inside {@code content} as wide as the widest needs, and
     * never narrower than the column's usual width.
     * <p>
     * The column was 96 pixels whatever it held, so a longer caption was cut
     * off: in French the Record Failure dialog's "Priorité de l'anomalie" and
     * "Gravité de l'anomalie" are about 125 pixels, and a tester could not read
     * which row was which (#66, finding 187). Widened one row at a time, the
     * rows would stop lining up; widened together, they still do.
     */
    static void align(final @NotNull JComponent content) {
        final @NotNull List<JComponent> captions = UIUtil.uiTraverser(content).filter(JComponent.class)
                .filter(component -> component.getClientProperty(NEEDS) instanceof Integer).toList();

        final int width = captions.stream()
                .mapToInt(caption -> (Integer) caption.getClientProperty(NEEDS))
                .reduce(JBUI.scale(96), Math::max);

        captions.forEach(caption -> caption.setPreferredSize(new Dimension(width, caption.getPreferredSize().height)));
    }
}
