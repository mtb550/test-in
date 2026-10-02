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
package org.testin.view.history;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.awt.BorderLayout;
import java.util.List;

public class HistoryTabIdeTest extends BasePlatformTestCase {

    // Rule-VIEW-PANEL-037
    public void testTheHistoryTabShowsOneLineSayingItIsNotBuilt() {
        final @NotNull JBPanel<?> tab = new JBPanel<>(new BorderLayout());
        new HistoryTab().load(tab);

        assertEquals("the History tab shows something other than its one line", List.of(Bundle.message("view.history.none")), Drawn.words(tab));
    }
}
