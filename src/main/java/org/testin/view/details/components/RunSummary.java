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
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.Badges;
import org.testin.util.Bundle;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public final class RunSummary extends BaseDetails {
    private static final int GAP = 6;
    private static final int INSETS_TOP = 8;
    private static final int INSETS_SIDE = 16;

    private final @NotNull TestRunItems item;

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull JBPanel<?> pills = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(GAP), 0));
        pills.setOpaque(false);

        Badges.showBadges(pills, facts());

        return addFullWidthRow(panel, gbc, pills, JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), currentRow);
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    private @NotNull List<Badges.Badge> facts() {
        final @NotNull List<Badges.Badge> badges = new ArrayList<>();
        badges.add(new Badges.Pill(item.shownStatus().getLabel(), item.shownStatus().getRowColor()));

        tag(badges, RunEditorAttributes.DURATION.getRunValueExtractor().apply(item));
        tag(badges, ranBy());

        return badges;
    }

    private @NotNull String ranBy() {
        final @NotNull String who = RunEditorAttributes.EXECUTED_BY.getRunValueExtractor().apply(item).trim();
        final @NotNull String when = RunEditorAttributes.EXECUTED_AT.getRunValueExtractor().apply(item).trim();

        if (who.isEmpty()) return when;
        if (when.isEmpty()) return who;

        return Bundle.message("details.ran.by", who, when);
    }

    private static void tag(final @NotNull List<Badges.Badge> badges, final @NotNull String text) {
        if (text.isBlank()) return;

        badges.add(new Badges.Tag(text, JBColor.GRAY));
    }
}
