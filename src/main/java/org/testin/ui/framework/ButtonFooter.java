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

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Optional;

// UC-INTERNAL-007, Rule-INTERNAL-080
final class ButtonFooter {
    private final @NotNull JBLabel reason = new JBLabel();
    private final @NotNull JBPanel<?> panel;

    ButtonFooter(final @NotNull JComponent button) {
        reason.setFont(Fonts.small());
        reason.setForeground(SimpleTextAttributes.ERROR_ATTRIBUTES.getFgColor());

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(8, 12));
        panel.add(reason, BorderLayout.WEST);
        panel.add(button, BorderLayout.EAST);
    }

    @NotNull JComponent getPanel() {
        return panel;
    }

    // Rule-INTERNAL-080
    void showReason(final @NotNull Optional<String> text) {
        reason.setText(text.orElse(""));
    }
}
