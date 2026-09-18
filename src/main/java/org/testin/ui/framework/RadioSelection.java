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
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.components.panels.HorizontalLayout;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * One captioned radio row — a caption above one radio button per option
 * (e.g. bug severity, bug priority). The dialog reads {@link #getSelected()}
 * on submit; a declared initial value keeps the selection always valid.
 */
public final class RadioSelection<T> implements DialogComponent {

    private final @NotNull JBPanel<?> panel;
    private final @NotNull JRadioButton firstButton;
    private @NotNull T selected;

    RadioSelection(final @NotNull String caption, final @NotNull List<Option<T>> options, final @NotNull T initial) {
        this.selected = initial;

        final @NotNull Font radioFont = JBFont.label().biggerOn(2f);
        final @NotNull ButtonGroup group = new ButtonGroup();
        // No gap before the first button, so it starts under its caption's
        // first letter; a flow layout puts its gap on the left edge too.
        final @NotNull JBPanel<?> radioRow = new JBPanel<>(new HorizontalLayout(JBUI.scale(8)));
        radioRow.setOpaque(false);

        Optional<JRadioButton> first = Optional.empty();
        for (final Option<T> option : options) {
            final @NotNull JRadioButton radio = new JRadioButton(option.name());
            radio.setFont(radioFont);
            radio.setOpaque(false);
            radio.setSelected(option.value().equals(initial));
            radio.addActionListener(event -> selected = option.value());
            group.add(radio);
            radioRow.add(radio);
            if (first.isEmpty()) first = Optional.of(radio);
        }
        // The builder guarantees at least one option.
        this.firstButton = first.orElseThrow();

        // Rule-INTERNAL-087
        panel = Caption.above(caption, radioRow);
    }

    public @NotNull T getSelected() {
        return selected;
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
        // Choosing an option is not a submit gesture; the declared keys save.
    }

    /**
     * One selectable option: the text on the radio and the submitted value.
     */
    record Option<T>(@NotNull String name, @NotNull T value) {
    }
}
