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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.codegen.AutomationState;
import org.testin.editor.BaseCard;
import org.testin.editor.CardHoverAction;
import org.testin.testrun.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.services.Services;

import java.awt.*;
import java.util.*;
import java.util.List;

public class RunCard extends BaseCard {
    private final @NotNull Project p;
    private final @NotNull List<Badges.Badge> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();

    public RunCard(final @NotNull Project p) {
        super();
        this.p = p;
    }

    // UC-EDITOR-PANEL-030
    public void updateData(final @NotNull Integer index, final @NotNull Set<?> activeDetails, final @NotNull TestRunItems runItem, final @NotNull String title) {
        this.automation = Services.getInstance(p, AutomationState.class).of(runItem.getId());

        badges.clear();
        details.clear();

        Arrays.stream(RunEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(runItem, badges, details, p));

        this.runSlot = CardHoverAction.runSlot(p, runItem.requireTc());

        updateUI(index, title, badges, details);

        // The status label is there only when the tester is showing that column.
        Optional.ofNullable(attributeLabels.get(RunEditorAttributes.RUN_STATUS.getName())).ifPresent(statusLabel -> {
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
            statusLabel.setForeground(runItem.getStatus().getRowColor());
        });

    }
}