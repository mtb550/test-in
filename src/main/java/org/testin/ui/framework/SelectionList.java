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
import org.testin.util.Icons;

import javax.swing.*;

/**
 * One selectable row of a {@link TextFieldWithSelections}: an icon, the text
 * shown to the tester, an optional muted hint explaining what the choice
 * means, and the value the dialog receives when this row is selected on
 * submit.
 * <p>
 * Public since #29. A dialog with fixed choices still declares them through
 * {@code ComponentDialogBase.textFieldWithSelections().selection(...)} and never
 * names this type; a dialog that searches has to build its rows as the tester
 * types, which is outside the framework, so the row it builds is this.
 */
public record SelectionList<T>(@NotNull Icon icon, @NotNull String name, @NotNull String hint, @NotNull T value) {

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-077.
     * <p>
     * Grayed here, so no caller has to remember and none of them can disagree:
     * the rows of one list are drawn by one component and should look like each
     * other. Once, when the row is built, rather than each time it is painted -
     * a list repaints its fifty rows on every keystroke.
     */
    public static <T> @NotNull SelectionList<T> add(final @NotNull Icon icon, final @NotNull String name, final @NotNull String hint, final @NotNull T value) {
        return new SelectionList<>(Icons.gray(icon), name, hint, value);
    }
}
