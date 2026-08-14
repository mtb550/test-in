package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * A ready-made panel as dialog content — a table, a tabbed pane, anything a
 * feature already builds for itself.
 * <p>
 * It exists so a dialog can be declared on the framework without its every
 * panel first becoming a framework component. It adds no behavior: it does not
 * take the initial focus, and it never submits.
 */
public final class PanelComponent implements IDialogComponent {

    private final @NotNull JComponent panel;
    private final boolean fillsSpace;

    PanelComponent(final @NotNull JComponent panel, final boolean fillsSpace) {
        this.panel = panel;
        this.fillsSpace = fillsSpace;
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
        // Nothing here submits; the dialog's button does.
    }

    /**
     * The panel is content, not input - the form above it keeps the focus.
     */
    @Override
    public boolean wantsFocus() {
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return fillsSpace;
    }
}
