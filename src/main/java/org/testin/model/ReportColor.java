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

package org.testin.model;

import com.intellij.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.function.Function;

public enum ReportColor {
    PAGE(
            Color.WHITE,
            ColorUtil.darker(Color.DARK_GRAY, 5),
            "page"
    ),

    INK(
            Color.BLACK,
            ColorUtil.brighter(Color.LIGHT_GRAY, 2),
            "ink"
    ),

    HEADING(
            Color.BLUE.darker().darker(),
            "heading"
    ),

    ACCENT(
            Color.BLUE.darker(),
            "accent"
    ),

    MUTED(
            Color.GRAY.darker(),
            Color.GRAY.brighter(),
            "muted"
    ),

    PANEL(
            ColorUtil.mix(Color.WHITE, Color.BLUE, 0.04),
            ColorUtil.darker(Color.DARK_GRAY, 2),
            "panel"
    ),

    LINE(
            ColorUtil.mix(Color.WHITE, Color.BLUE, 0.15),
            Color.DARK_GRAY,
            "line"
    ),

    FOOTER_INK(
            Color.GRAY,
            Color.GRAY,
            "footer-ink"
    ),

    LINK(
            Color.BLUE.darker(),
            "link"
    );

    private final @NotNull Color onLightPage;
    private final @NotNull Color onDarkPage;
    private final @NotNull String cssName;

    ReportColor(final @NotNull Color onLightPage, final @NotNull String cssName) {
        this(onLightPage, forDarkPage(onLightPage), cssName);
    }

    ReportColor(final @NotNull Color onLightPage, final @NotNull Color onDarkPage, final @NotNull String cssName) {
        this.onLightPage = onLightPage;
        this.onDarkPage = onDarkPage;
        this.cssName = cssName;
    }

    // Rule-REPORT-025
    public static @NotNull Color forDarkPage(final @NotNull Color onLightPage) {
        return ColorUtil.mix(Color.WHITE, onLightPage, 0.5);
    }

    public static @NotNull String cssTokens(final @NotNull Function<ReportColor, String> shade) {
        final @NotNull StringBuilder tokens = new StringBuilder();

        for (final ReportColor color : values()) {
            tokens.append("--").append(color.cssName).append(": #").append(shade.apply(color)).append(";");
        }

        return tokens.toString();
    }

    public @NotNull String hex() {
        return ColorUtil.toHex(onLightPage);
    }

    public @NotNull String darkHex() {
        return ColorUtil.toHex(onDarkPage);
    }
}
