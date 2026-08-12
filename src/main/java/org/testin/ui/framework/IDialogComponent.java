package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The contract every framework dialog component follows. A component owns its
 * own layout and internal behaviour (navigation, selection sync); the dialog
 * owns the title, the status bar and the shortcut-to-action mapping.
 */
public interface IDialogComponent {

    /**
     * The component's whole panel, placed in the dialog center.
     */
    @NotNull JComponent getPanel();

    /**
     * Where focus goes and where the dialog binds its declared shortcuts.
     */
    @NotNull JComponent getFocusComponent();

    /**
     * Registers what the dialog runs when the component itself asks to submit
     * (e.g. a mouse click on a selection).
     */
    void onSubmitRequest(@NotNull Runnable submit);
}
