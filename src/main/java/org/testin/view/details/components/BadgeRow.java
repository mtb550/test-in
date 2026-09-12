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

package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;

import java.awt.*;
import java.util.List;

public class BadgeRow extends BaseDetails {

    final int FLOW_GAP = 6;
    final int INSETS_TOP = 8;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 16;
    final int INSETS_RIGHT = 16;

    // UC-VIEW-PANEL-004
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull JBPanel<?> badgesPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(FLOW_GAP), 0));
        badgesPanel.setOpaque(false);

        final @NotNull List<Badges.Badge> badges = Badges.caseBadges(dto);

        // Last, the way a card orders them. None for a case nobody has run.
        final @NotNull RunStatus tempStatus = Services.getInstance(p, TestNGExecution.class).statusOf(dto);
        if (tempStatus.hasBadge()) badges.add(Badges.createRunStatusBadge(tempStatus.getBadge()));

        Badges.showBadges(badgesPanel, badges);

        return addFullWidthRow(panel, gbc, badgesPanel,
                JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT), currentRow);
    }
}