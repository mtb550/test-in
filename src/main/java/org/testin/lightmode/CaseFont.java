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

package org.testin.lightmode;

import com.intellij.util.ui.JBFont;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.FontSync;

import java.awt.Font;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CaseFont {
    private static final int BIGGER = 3;
    private static final int SMALLER = 2;

    static @NotNull Font description() {
        return at(FontSync.getBaseFontSize() + BIGGER, Font.BOLD);
    }

    static @NotNull Font body() {
        return at(FontSync.getBaseFontSize(), Font.PLAIN);
    }

    static @NotNull Font label() {
        return at(FontSync.getBaseFontSize() - SMALLER, Font.PLAIN);
    }

    private static @NotNull Font at(final float size, @MagicConstant(flags = {Font.PLAIN, Font.BOLD, Font.ITALIC}) final int style) {
        return JBFont.label().deriveFont(style, Math.max(FontSync.FLOOR, size));
    }

    static @NotNull Font zoomed(final @NotNull Font base, final float zoom) {
        return base.deriveFont(base.getSize2D() * zoom);
    }
}
