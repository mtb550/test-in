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
public final class DialogButton implements IDialogComponent {

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

    /** Working dialogs disable the button while the input is incomplete. */
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
}
