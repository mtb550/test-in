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
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Html;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Component;

public final class DialogMessage implements DialogComponent {
    private final @NotNull JBPanel<?> panel;

    DialogMessage(final @NotNull String text, final @NotNull String from, final @NotNull String to) {
        final @NotNull JBPanel<?> content = new JBPanel<>();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        final @NotNull JBLabel message = new JBLabel("<html>" + Html.ofText(text) + "</html>");
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(message);

        addPathRow(content, Bundle.message("caption.from"), from, 8);
        addPathRow(content, Bundle.message("caption.to"), to, 2);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(12));
        panel.setFocusable(true);
        panel.add(content, BorderLayout.CENTER);
    }

    private static void addPathRow(final @NotNull JBPanel<?> content, final @NotNull String caption, final @NotNull String path, final int topGap) {
        if (path.isEmpty()) return;

        final @NotNull JBLabel label = new JBLabel(caption + ":  " + path);
        label.setFont(JBUI.Fonts.label());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        label.setBorder(JBUI.Borders.emptyTop(topGap));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(label);
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return panel;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
