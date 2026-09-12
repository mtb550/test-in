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

package org.testin.view;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.CardHoverAction;

import javax.swing.JComponent;
import java.util.List;

public class ViewPanelActions {

    /**
     * UC-VIEW-PANEL-003, UC-VIEW-PANEL-012, UC-VIEW-PANEL-014.
     * <p>
     * What the panel answers: the two buttons in its title bar, and the two keys
     * its own tooltips name.
     * <p>
     * The card actions are bound and not returned. Their buttons are already
     * drawn inside the Details tab, tooltips and all - what was missing was the
     * keys those tooltips promised, which were bound on the editor's card list
     * and nowhere else (#225).
     */
    public @NotNull List<AnAction> create(final @NotNull ViewPanel panel, final @NotNull JComponent component) {
        ShownCaseAction.bind(panel, CardHoverAction.NAVIGATE_TO_TEST_METHOD, component);
        ShownCaseAction.bind(panel, CardHoverAction.RUN_TEST_CASE, component);

        return List.of(
                new PreviousTestCaseAction(panel.getPage(), component),
                new NextTestCaseAction(panel.getPage(), component)
        );
    }
}
