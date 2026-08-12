package org.testin.ui.framework;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * One captioned radio row — a muted caption and one radio button per option
 * (e.g. bug severity, bug priority). The dialog reads {@link #getSelected()}
 * on submit; a declared initial value keeps the selection always valid.
 */
public final class RadioSelection<T> implements IDialogComponent {

    /** One selectable option: the text on the radio and the submitted value. */
    record Option<T>(@NotNull String name, @NotNull T value) {
    }

    private final @NotNull JBPanel<?> panel;
    private final @NotNull JRadioButton firstButton;
    private @NotNull T selected;

    RadioSelection(final @NotNull String caption, final @NotNull List<Option<T>> options, final @NotNull T initial) {
        this.selected = initial;

        final Font radioFont = JBFont.label().biggerOn(2f);
        final ButtonGroup group = new ButtonGroup();
        final JBPanel<?> radioRow = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radioRow.setOpaque(false);

        JRadioButton first = null;
        for (final Option<T> option : options) {
            final JRadioButton radio = new JRadioButton(option.name());
            radio.setFont(radioFont);
            radio.setOpaque(false);
            radio.setSelected(option.value().equals(initial));
            radio.addActionListener(event -> selected = option.value());
            group.add(radio);
            radioRow.add(radio);
            if (first == null) first = radio;
        }
        // The builder guarantees at least one option.
        this.firstButton = Objects.requireNonNull(first);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(Captions.panel(caption), BorderLayout.WEST);
        panel.add(radioRow, BorderLayout.CENTER);
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
}
