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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.util.Bundle;

@Getter
@AllArgsConstructor
public enum LightModePart implements ToolBarAttribute {
    SET_NAME(
            Bundle.message("light.part.set.name")
    ),

    DURATION(
            Bundle.message("light.part.duration")
    ),

    VERDICT_BUTTONS(
            Bundle.message("light.part.verdict.buttons")
    ),

    STATUS_BAR(
            Bundle.message("light.part.status.bar")
    );

    private final @NotNull String name;

    @Override
    public @NotNull ToolBarDefault getToolBarDefault() {
        return ToolBarDefault.ON;
    }
}
