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

/**
 * The parts of the light mode window a tester can turn off (#13).
 * <p>
 * <b>Four, and it cannot grow past what the window holds.</b> Every one of
 * these is a thing already on screen, so this is a menu on the title bar rather
 * than a page in Settings - there is nothing here to configure that is not
 * visible from the window it configures.
 * <p>
 * <b>Neither the description nor the expected result is among them.</b> They are
 * one thought and the window exists to show it: the description says what to do
 * and the expected result says what should happen, and a tester who can see only
 * the first has no way to judge it. A window showing half a test case is not a
 * smaller window, it is a broken one. Both are left off the list entirely rather
 * than listed and locked, because a checkbox that cannot be unticked invites the
 * tester to try.
 * <p>
 * <b>The status bar is on the list, and stays on it.</b> A tester who has learned
 * the keys does not need a strip repeating them, and the window is small enough
 * that a row they never read is worth reclaiming. The one case where it is not
 * theirs to hide is a failure form - with no buttons on it, that row is the only
 * place Enter and Escape are written down - and {@code LightModeWindow} forces it
 * back for exactly that state rather than taking the choice away everywhere.
 * <p>
 * <b>Duration is one entry rather than two.</b> The two clocks are one line;
 * hiding one and keeping the other would leave a lopsided row and a toggle
 * nobody would reach for twice.
 * <p>
 * All start on. A tester who has never opened the menu sees the whole window,
 * and what they tick is remembered per machine rather than per project - the
 * same person at the same screen wants the same window, whichever project they
 * open.
 */
@Getter
@AllArgsConstructor
public enum LightModePart implements ToolBarAttribute {

    SET_NAME(Bundle.message("light.part.set.name")),
    DURATION(Bundle.message("light.part.duration")),
    VERDICT_BUTTONS(Bundle.message("light.part.verdict.buttons")),
    STATUS_BAR(Bundle.message("light.part.status.bar"));

    private final @NotNull String name;

    @Override
    public @NotNull ToolBarDefault getToolBarDefault() {
        return ToolBarDefault.ON;
    }
}
