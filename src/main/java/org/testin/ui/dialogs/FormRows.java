package org.testin.ui.dialogs;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * A label/field grid for dialog forms: labels in a narrow left column, fields
 * taking the rest of the width, one row per call.
 * <p>
 * The forms that ask for a file each repeated the same GridBag constraints.
 * Here they live once, so a form reads as the rows it has and the two dialogs
 * a tester sees side by side line their labels up the same way.
 */
public final class FormRows extends JBPanel<FormRows> {

    private final @NotNull GridBagConstraints gbc = new GridBagConstraints();

    private int nextRow;

    public FormRows() {
        super(new GridBagLayout());
        setOpaque(false);

        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
    }

    /**
     * A row whose label column stays empty - how a checkbox lines up under the
     * fields above it.
     */
    public @NotNull FormRows unlabeledRow(final @NotNull JComponent field) {
        return row("", field);
    }

    /**
     * One label/field row.
     */
    public @NotNull FormRows row(final @NotNull String label, final @NotNull JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = nextRow;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        add(label.isEmpty() ? new JBPanel<>() : new JBLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(field, gbc);

        nextRow++;
        return this;
    }

    /**
     * A row spanning both columns - a note under the fields, not a field of
     * its own.
     */
    public @NotNull FormRows wideRow(final @NotNull JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = nextRow;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        add(component, gbc);

        nextRow++;
        return this;
    }
}
