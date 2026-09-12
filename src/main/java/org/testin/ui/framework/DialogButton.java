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

/**
 * A confirm button row — for working dialogs where a visible OK button reads
 * better than an Enter hint. Clicking it runs the dialog's submit action; the
 * button is the default-styled primary action, right-aligned.
 */
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

    /**
     * Working dialogs disable the button while the input is incomplete.
     */
    public void setEnabled(final boolean enabled) {
        button.setEnabled(enabled);
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

    /**
     * A button row is the wrong thing to hand spare space to.
     * <p>
     * Without this, a dialog that declares no filler at all - a form and a
     * button, say - gave the space to its last component, which is this one. The
     * button then drifted into the middle instead of sitting at the bottom.
     */
    @Override
    public boolean canFillSpace() {
        return false;
    }
}
