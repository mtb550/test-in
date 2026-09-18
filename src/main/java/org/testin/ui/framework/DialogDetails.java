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

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Read-only context rows, each value named by a caption above it or by an icon
 * before it (e.g. the test case's description and expected result above an
 * input). Display only: never takes the focus, never submits.
 */
public final class DialogDetails implements DialogComponent {

    private final @NotNull JBPanel<?> panel;

    DialogDetails(final @NotNull List<Row> rows) {
        final @NotNull JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.setBorder(JBUI.Borders.empty(4, 0));

        for (final Row row : rows) {
            // Rule-INTERNAL-087. The icon in the middle of the value's height,
            // as the test case form draws a field's icon beside its box.
            final @NotNull JBLabel value = wrappingValue(row.value());
            row.icon().ifPresent(icon -> {
                value.setIcon(icon);
                value.setIconTextGap(JBUI.scale(8));
            });
            stack.add(Caption.above(row.caption(), value));
        }

        // Anchored to the top: when the rows are all a dialog holds, and it is
        // taller than they are, a bare box layout would spread them down the
        // dialog. Rows keep their height and the room stays below them.
        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(stack, BorderLayout.NORTH);
    }

    /**
     * Long values wrap instead of widening the whole dialog. At the framework's
     * text edge, 12 pixels in, like the text of its fields.
     */
    private static @NotNull JBLabel wrappingValue(final @NotNull String value) {
        final @NotNull JBLabel label = new JBLabel("<html><div style='width:" + JBUI.scale(420) + "px'>"
                + StringUtil.escapeXmlEntities(value) + "</div></html>");
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
        // Display only - nothing to submit.
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    /**
     * One row: a value named by its caption, or by its icon and no caption.
     */
    record Row(@NotNull String caption, @NotNull Optional<Icon> icon, @NotNull String value) {
    }
}
