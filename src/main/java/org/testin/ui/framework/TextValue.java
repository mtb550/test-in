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
