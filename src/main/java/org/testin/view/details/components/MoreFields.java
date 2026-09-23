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

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.awt.GridBagConstraints;
import java.util.List;

public final class MoreFields extends BaseDetails {
    private static final @NotNull String OPEN = "testin.viewPanel.moreOpen";
    private static final int INSETS_TOP = 10;
    private static final int INSETS_SIDE = 16;
    private static final int INSETS_BOTTOM = 16;

    private final @NotNull List<BaseDetails> folded;

    public MoreFields(final @NotNull List<BaseDetails> folded) {
        this.folded = List.copyOf(folded);
    }

    // Rule-VIEW-PANEL-087
    private static boolean isOpen() {
        return PropertiesComponent.getInstance().getBoolean(OPEN, false);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-087
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        int row = currentRow;

        if (isOpen()) {
            for (final BaseDetails field : folded) {
                row = field.render(p, panel, (GridBagConstraints) gbc.clone(), dto, row);
            }
        }

        return addFullWidthRow(panel, gbc, toggle(p), JBUI.insets(INSETS_TOP, INSETS_SIDE, INSETS_BOTTOM, INSETS_SIDE), row);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-087
    private @NotNull ActionLink toggle(final @NotNull Project p) {
        final @NotNull ActionLink link = link(Bundle.message("details.more"), _ -> {
            PropertiesComponent.getInstance().setValue(OPEN, !isOpen(), false);
            ViewToolWindowFactory.panel(p).ifPresent(ViewPanel::refreshCurrentView);
        });

        link.setIcon(isOpen() ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight);

        return link;
    }
}
