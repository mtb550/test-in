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


package org.testin.help;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.function.Supplier;

public record Hint(@NotNull Guide topic, @NotNull String text, @NotNull Optional<Supplier<JComponent>> form) {
    // UC-INTERNAL-009, Rule-INTERNAL-127
    public static @NotNull Hint of(final @NotNull Guide topic, final @NotNull String text) {
        return new Hint(topic, text, Optional.empty());
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public static @NotNull Hint of(final @NotNull Guide topic, final @NotNull String text, final @NotNull Supplier<JComponent> form) {
        return new Hint(topic, text, Optional.of(form));
    }
}
