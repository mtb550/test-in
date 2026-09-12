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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * The framework's caption column: platform hint style (small, gray), fixed
 * width so captioned rows align across components.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Captions {

    static @NotNull JBPanel<?> panel(final @NotNull String caption) {
        final @NotNull JBPanel<?> captionPanel = new JBPanel<>(new GridBagLayout());
        captionPanel.setOpaque(false);

        final @NotNull JBLabel label = new JBLabel(caption);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        captionPanel.add(label);

        captionPanel.setPreferredSize(new Dimension(JBUI.scale(96), captionPanel.getPreferredSize().height));
        return captionPanel;
    }
}
