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

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public final class DialogButton implements DialogComponent {
    private final @NotNull JButton button;
    private final @NotNull JBPanel<?> panel;
    private @NotNull Runnable submitRequest = () -> {
    };

    DialogButton(final @NotNull String text) {
        button = new JButton(text);
        button.addActionListener(event -> submitRequest.run());

        panel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(8, 12));
        panel.add(button);
    }

    public void setEnabled(final boolean enabled) {
        button.setEnabled(enabled);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-080
    public void enableUnless(final @NotNull Optional<String> reason) {
        button.setEnabled(reason.isEmpty());
        button.setToolTipText(reason.orElse(""));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return button;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        this.submitRequest = submit;
    }

    @Override
    public boolean canFillSpace() {
        return false;
    }
}
