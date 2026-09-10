package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;

/**
 * UC-INTERNAL-007, Rule-INTERNAL-067.
 * <p>
 * A component the tester types a value into, and which can say that value is
 * the one holding the dialog open.
 * <p>
 * Two components answer both questions - {@link TextInput} and
 * {@link TextFieldWithSelections} - and every dialog built on either of them
 * wrote the same two steps out again: trim what was typed, warn if it is empty,
 * then ask whatever else that dialog cares about and refuse. Six dialogs, the
 * same six lines, agreeing only because nobody had changed one of them yet
 * (#11).
 * <p>
 * Named so the shell can run those steps once. It carries no method either
 * class did not already have.
 */
public interface TextValue {

    /**
     * What the field holds, as the tester left it. The shell trims it; a
     * component does not decide what a surrounding space means.
     */
    @NotNull String getText();

    /**
     * Says this field is the empty one, in the words the field itself carries -
     * see {@link EmptyWarning}.
     */
    void showEmptyWarning();
}
