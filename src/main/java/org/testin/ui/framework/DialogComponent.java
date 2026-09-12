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

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The contract every framework dialog component follows. A component owns its
 * own layout and internal behavior (navigation, selection sync); the dialog
 * owns the title, the status bar and the shortcut-to-action mapping.
 */
public interface DialogComponent {

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
     * claims the space, the last component that {@link #canFillSpace()} fills.
     */
    default boolean fillsSpace() {
        return false;
    }

    /**
     * False for a component that must keep its preferred height even when
     * nothing else claims the dialog's space - a button row belongs at the
     * bottom, not stretched down the middle of a dialog with room to spare.
     */
    default boolean canFillSpace() {
        return true;
    }
}
