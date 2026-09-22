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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.CardHoverAction;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.util.Bundle;

import java.util.List;

@Getter
@AllArgsConstructor
public enum LightModePart implements ToolBarAttribute {
    SET_NAME(
            Bundle.message("light.part.set.name"),
            List.of()
    ),

    DURATION(
            Bundle.message("light.part.duration"),
            List.of()
    ),

    VERDICT_BUTTONS(
            Bundle.message("light.part.verdict.buttons"),
            List.of()
    ),

    STATUS_BAR(
            Bundle.message("light.part.status.bar"),
            List.of()
    ),

    TEST_METHOD_BUTTON(
            Bundle.message("action.Testin.NavigateToTestMethod.text"),
            List.of(CardHoverAction.NAVIGATE_TO_TEST_METHOD)
    ),

    RUN_BUTTON(
            Bundle.message("action.Testin.RunTestMethod.text"),
            List.of(CardHoverAction.RUN_TEST_METHOD, CardHoverAction.STOP_TEST_METHOD)
    ),

    TEST_CASE_BUTTON(
            Bundle.message("action.Testin.NavigateToTestCase.text"),
            List.of(CardHoverAction.NAVIGATE_TO_TEST_CASE)
    );

    private final @NotNull String name;

    @Getter(AccessLevel.NONE)
    private final @NotNull List<CardHoverAction> buttons;

    @Override
    public @NotNull ToolBarDefault getToolBarDefault() {
        return ToolBarDefault.ON;
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-244
    public boolean governs(final @NotNull CardHoverAction button) {
        return buttons.contains(button);
    }
}
