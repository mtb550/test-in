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

package org.testin.editor.grid;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridKeys {
    public static final @NotNull String COPY = "testin.grid.copy";
    public static final @NotNull String CUT = "testin.grid.cut";
    public static final @NotNull String PASTE = "testin.grid.paste";

    // UC-EDITOR-PANEL-018
    public static @NotNull Map<KeyStroke, String> clipboard() {
        final int menuMask = Shortcuts.menuMask();

        return Map.of(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), COPY,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), CUT,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), PASTE);
    }

    public static @NotNull KeyStroke enter() {
        return Shortcuts.Enter.getKey();
    }

    public static @NotNull Set<KeyStroke> keptFromMenus() {
        final @NotNull Set<KeyStroke> kept = new HashSet<>(clipboard().keySet());
        kept.add(enter());

        return Set.copyOf(kept);
    }
}
