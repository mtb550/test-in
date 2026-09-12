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

package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.testrun.RunEditorAttributes;

public class RunDetailsPopupBtn extends AbstractDetailsPopupBtn<RunEditorAttributes> {

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public RunDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // The key is bumped only when a stored selection would be answering an
        // older question, because bumping discards what every tester ticked: v5
        // was Executed By and Executed At joining (#27), v7 the curated defaults
        // (#80). Order needed none - it is LOCKED_CHECKED, and a locked attribute
        // is forced into whatever was stored when the popup loads it.
        super(FIELDS,
                "testin.selectedDetails.run.v7",
                RunEditorAttributes.class,
                onToolBarDetailsSelectedChanged);
    }
}
