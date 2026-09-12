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

package org.testin.util;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

/**
 * What a list has selected.
 * <p>
 * Swing says "nothing is selected" with a null, and five places asked about it
 * separately - a copy action, a run-item action, a shortcut menu and a field
 * that falls back to its first row. Converted here, so an empty selection reads
 * as the same thing everywhere it is read.
 * <p>
 * The tree's side of this is {@link org.testin.explorer.tree.TreeValues}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListValue {

    public static <T> @NotNull Optional<T> selected(final @NotNull JList<T> list) {
        return Optional.ofNullable(list.getSelectedValue());
    }
}
