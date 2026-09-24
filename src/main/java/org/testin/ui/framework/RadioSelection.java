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
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Fonts;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RadioSelection<T> implements DialogComponent {
    private final @NotNull JBPanel<?> panel;
    private final @NotNull JRadioButton firstButton;
    private final @NotNull Map<T, JRadioButton> buttons = new LinkedHashMap<>();
    private @NotNull T selected;

    RadioSelection(final @NotNull String caption, final @NotNull List<Option<T>> options, final @NotNull T initial) {
        this.selected = initial;

        final @NotNull ButtonGroup group = new ButtonGroup();
        final @NotNull JBPanel<?> radioRow = new JBPanel<>(new HorizontalLayout(8));
        radioRow.setOpaque(false);
        radioRow.setBorder(JBUI.Borders.emptyLeft(12));

        Optional<JRadioButton> first = Optional.empty();
        for (final Option<T> option : options) {
            final @NotNull JRadioButton radio = new JRadioButton(option.name());
            DialogStyle.asChoice(radio);
            radio.setOpaque(false);
            radio.setSelected(option.value().equals(initial));
            radio.addActionListener(_ -> selected = option.value());
            group.add(radio);
            radioRow.add(radio);
            buttons.put(option.value(), radio);
            if (first.isEmpty()) first = Optional.of(radio);
        }
        this.firstButton = first.orElseThrow();

        // Rule-INTERNAL-087
        panel = Caption.above(caption, radioRow);
    }

    public @NotNull T getSelected() {
        return selected;
    }

    public void select(final @NotNull T value) {
        Optional.ofNullable(buttons.get(value)).ifPresent(radio -> {
            radio.setSelected(true);
            selected = value;
        });
    }

    public void setEditable(final boolean editable) {
        buttons.values().forEach(radio -> radio.setEnabled(editable));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return firstButton;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    record Option<T>(@NotNull String name, @NotNull T value) {
    }
}
