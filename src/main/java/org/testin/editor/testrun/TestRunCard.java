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

package org.testin.editor.testrun;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.card.BaseCard;
import org.testin.model.TestRunItems;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.Badge;
import org.testin.util.Fonts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TestRunCard extends BaseCard {
    private final @NotNull List<Badge> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();

    public TestRunCard(final @NotNull Project p) {
        super(p);
    }

    // UC-EDITOR-PANEL-030
    public void updateData(final @NotNull Integer index, final @NotNull Set<TestRunEditorAttributes> activeDetails, final @NotNull TestRunItems runItem, final @NotNull String title) {
        this.automation = automationState.of(runItem.getId());

        badges.clear();
        details.clear();

        Arrays.stream(TestRunEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(runItem, badges, details));

        updateUI(index, title, badges, details);

        Optional.ofNullable(attributeLabels.get(TestRunEditorAttributes.RUN_STATUS.getName())).ifPresent(statusLabel -> {
            statusLabel.setFont(Fonts.strong());
            statusLabel.setForeground(runItem.shownStatus().getRowColor());
        });
    }
}