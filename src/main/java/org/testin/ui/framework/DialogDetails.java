package org.testin.ui.framework;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Read-only context rows — a muted caption column and the value beside it
 * (e.g. the test case's description and expected result above an input).
 * Display only: never takes the focus, never submits.
 */
public final class DialogDetails implements DialogComponent {

    private final @NotNull JBPanel<?> panel;

    DialogDetails(final @NotNull List<Row> rows) {
        final JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.setBorder(JBUI.Borders.empty(4, 0));

        for (final Row row : rows) {
            final JBPanel<?> rowPanel = new JBPanel<>(new BorderLayout());
            rowPanel.setOpaque(false);
            rowPanel.setBorder(JBUI.Borders.emptyTop(8));
            rowPanel.add(Captions.panel(row.caption()), BorderLayout.WEST);
            rowPanel.add(wrappingValue(row.value()), BorderLayout.CENTER);
            stack.add(rowPanel);
        }

        // Anchored to the top: when the rows are all a dialog holds, and it is
        // taller than they are, a bare box layout would spread them down the
        // dialog. Rows keep their height and the room stays below them.
        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(stack, BorderLayout.NORTH);
    }

    /**
     * Long values wrap instead of widening the whole dialog.
     */
    private static @NotNull JBLabel wrappingValue(final @NotNull String value) {
        return new JBLabel("<html><div style='width:" + JBUI.scale(420) + "px'>"
                + StringUtil.escapeXmlEntities(value) + "</div></html>");
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
     * One caption/value row.
     */
    record Row(@NotNull String caption, @NotNull String value) {
    }
}
