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

package org.testin.editor.run;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.*;

/**
 * UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-035.
 * <p>
 * The two controls of a manual execution, and what each is called and drawn
 * with, wherever it is offered: the run editor's toolbar and light mode's
 * title bar (#13). A tester looking at one and then the other has to see the
 * same pair of buttons.
 * <p>
 * Their own type. They sat on the {@code Toolbar} callbacks interface, so light
 * mode imported the toolbar's interface to name a button (#312, A27).
 */
@Getter
@AllArgsConstructor
public enum ExecutionControl {

    /**
     * "Manual" is in the name because the gesture runs no automation: it walks
     * the run a case at a time and times the tester reading each one. The
     * toolbar button called it "Start Execution" and the context menu entry
     * "Start Run", which read as the thing beside it in that menu - Run Test
     * Case, which does run code - and a tester who picked the wrong one watched
     * the first case get selected and a clock start while nothing ran.
     * <p>
     * Not the platform's run arrow, {@code AllIcons.Actions.Execute}: that is
     * the family Run Test Case draws from, so the two entries agreed in the one
     * way that mattered and disagreed in every other.
     */
    START(
            Bundle.message("toolbar.start.manual.execution"),
            AllIcons.Toolwindows.ToolWindowRun
    ),

    STOP(
            Bundle.message("toolbar.stop.execution"),
            AllIcons.Debugger.ThreadFrozen
    );

    private final @NotNull String label;
    private final @NotNull Icon icon;
}
