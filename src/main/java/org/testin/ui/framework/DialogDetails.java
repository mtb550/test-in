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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.util.Fonts;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

public final class DialogDetails implements DialogComponent {
    private final @NotNull JBPanel<?> panel;

    DialogDetails(final @NotNull List<Row> rows) {
        final @NotNull JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.setBorder(JBUI.Borders.empty(4, 0));

        for (final Row row : rows) {
            // Rule-INTERNAL-087
            final @NotNull JBLabel value = wrappingValue(row.value());
            row.icon().ifPresent(icon -> {
                value.setIcon(icon);
                value.setIconTextGap(JBUI.scale(8));
            });
            stack.add(Caption.above(row.caption(), value));
        }

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(stack, BorderLayout.NORTH);
    }

    // Rule-INTERNAL-096
    private static @NotNull JBLabel wrappingValue(final @NotNull String value) {
        final @NotNull JBLabel label = new JBLabel("<html><div style='width:" + JBUI.scale(420) + "px'>"
                + StringUtil.escapeXmlEntities(value) + "</div></html>");
        label.setFont(Fonts.value());
        label.setBorder(JBUI.Borders.emptyLeft(12));
        return label;
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

    @Override
    public boolean wantsFocus() {
        return false;
    }

    record Row(@NotNull String caption, @NotNull Optional<Icon> icon, @NotNull String value) {
    }
}
