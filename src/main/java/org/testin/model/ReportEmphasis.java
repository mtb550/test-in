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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum ReportEmphasis {
    ALARMING(
            "C0392B",
            "var(--verdict-failed)"
    ),

    CAUTIONARY(
            "B8860B",
            "var(--verdict-blocked)"
    ),

    MUTED(
            "595959",
            "var(--muted)"
    );

    private final @NotNull String hexColor;
    private final @NotNull String cssToken;
}
