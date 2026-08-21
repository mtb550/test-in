package org.testin.ui.framework;

import com.intellij.ui.components.JBOptionButton;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * A primary action with alternatives behind an arrow — the IDE's own commit
 * control, which is what a tester already knows: the common answer is on the
 * button and the others are one click away, rather than in a menu somewhere
 * else or a second dialog afterward.
 * <p>
 * Every action submits the dialog. Which one was pressed is a question the
 * dialog asks afterward through {@link #getChosen()}, so there is one submit
 * path with one validation in it, and the choice only decides what happens with
 * what the dialog collected.
 * <p>
 * The first label is the default: it is what the button shows, what a click
 * runs, and what the dialog's Enter gesture runs, so the key and the button
 * cannot mean two different things.
 */
public final class DialogSplitButton implements DialogComponent {

    private final @NotNull JBOptionButton button;
    private final @NotNull JBPanel<?> panel;
    private @NotNull String chosen;
    private @NotNull Runnable submitRequest = () -> {
    };

    DialogSplitButton(final @NotNull List<String> labels) {
        if (labels.isEmpty()) throw new IllegalStateException("A split button needs at least one action");

        chosen = labels.getFirst();

        final @NotNull Action main = action(labels.getFirst());
        final Action @NotNull[] alternatives = labels.stream().skip(1).map(this::action).toArray(Action[]::new);

        button = new JBOptionButton(main, alternatives.length == 0 ? null : alternatives);

        panel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(8, 12));
        panel.add(button);
    }

    /**
     * The label the tester pressed. Read inside the dialog's submit, which is
     * the only thing that needs to know.
     */
    public @NotNull String getChosen() {
        return chosen;
    }

    public void setEnabled(final boolean enabled) {
        button.setEnabled(enabled);
    }

    private @NotNull Action action(final @NotNull String label) {
        return new AbstractAction(label) {
            @Override
            public void actionPerformed(final ActionEvent event) {
                chosen = label;
                submitRequest.run();
            }
        };
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return button;
    }

    /**
     * Enter submits the way the default action does, so the chosen label is
     * left as it is - the first one, unless the tester picked another.
     */
    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        this.submitRequest = submit;
    }

    /**
     * A button row is the wrong thing to hand spare space to; see
     * {@link DialogButton#canFillSpace()}.
     */
    @Override
    public boolean canFillSpace() {
        return false;
    }
}
