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
import org.testin.testcase.TestEditorAttributes;

public class TestDetailsPopupBtn extends AbstractDetailsPopupBtn<TestEditorAttributes> {

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public TestDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // v4 is the curated default set (#80). See RunDetailsPopupBtn for when
        // this is bumped and what it costs.
        super(FIELDS,
                "testin.selectedDetails.test.v4",
                TestEditorAttributes.class,
                onToolBarDetailsSelectedChanged);
    }
}
