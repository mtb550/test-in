package org.testin.ui.framework;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * A plain message — the component of the confirmation dialogs. Multi-line via
 * {@code \n}; optional muted "From/To" rows show where a transfer goes. The
 * panel itself takes the focus so the dialog's declared keys work immediately.
 */
public final class DialogMessage implements DialogComponent {

    private final @NotNull JBPanel<?> panel;

    DialogMessage(final @NotNull String text, final @NotNull String from, final @NotNull String to) {
        final @NotNull JBPanel<?> content = new JBPanel<>();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        final @NotNull JBLabel message = new JBLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(message);

        addPathRow(content, "From", from, 8);
        addPathRow(content, "To", to, 2);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(12));
        panel.setFocusable(true);
        panel.add(content, BorderLayout.CENTER);
    }

    /**
     * A muted "From: path" row, or nothing at all when the message does not name
     * that side. One place decides whether the row exists, so both calls above
     * are unconditional.
     */
    private static void addPathRow(final @NotNull JBPanel<?> content, final @NotNull String caption,
                                   final @NotNull String path, final int topGap) {
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
        // A message has no submit gesture of its own; the declared keys do it.
    }
}
