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
     * (e.g. a mouse click on a selection, or an OK button).
     */
    void onSubmitRequest(@NotNull Runnable submit);

    /**
     * False for pure display components (context rows): they never take the
     * initial focus and never become the dialog's primary component.
     */
    default boolean wantsFocus() {
        return true;
    }

    /**
     * False for components whose focus keys must stay their own (e.g. a
     * multi-line area where Enter inserts a newline): the dialog's declared
     * keys are not installed on their focus component. The content-panel
     * bindings still apply for keys the component itself does not consume.
     */
    default boolean acceptsDialogKeys() {
        return true;
    }

    /**
     * True for the component that fills the dialog's remaining space (e.g. a
     * selection tree). Components above it keep their preferred height,
     * components below it (e.g. a button row) sit at the bottom. When none
     * claims the space, the last component fills.
     */
    default boolean fillsSpace() {
        return false;
    }
}
