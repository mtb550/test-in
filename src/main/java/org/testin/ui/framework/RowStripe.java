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

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RowStripe {
    private static final @NotNull Color EVEN = new JBColor(Gray._245, Gray._60);
    private static final @NotNull Color ODD = new JBColor(Gray._230, Gray._45);

    public static @NotNull Color of(final int index) {
        return index % 2 == 0 ? EVEN : ODD;
    }

    public static @NotNull Color odd() {
        return ODD;
    }
}
