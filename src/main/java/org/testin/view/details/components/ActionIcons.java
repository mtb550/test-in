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
import org.testin.codegen.AutomationState;
import org.testin.editor.CardHoverAction;
import org.testin.editor.HoverButton;
import org.testin.model.Automated;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.Box;
import javax.swing.JComponent;
import java.awt.FlowLayout;
import java.util.List;

public final class ActionIcons {
    private static final int STRUT_WIDTH = 8;

    // UC-VIEW-PANEL-009, UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-050, Rule-VIEW-PANEL-056, Rule-VIEW-PANEL-057, Rule-VIEW-PANEL-063
    public static @NotNull JComponent of(final @NotNull Project p, final @NotNull TestCaseDto dto) {
        final @NotNull CardHoverAction.Offered navigate = CardHoverAction.NAVIGATE_TO_TEST_METHOD.offer(p, dto);
        final @NotNull CardHoverAction.Offered run = CardHoverAction.RUN_TEST_METHOD.offer(p, dto);
        final @NotNull CardHoverAction.Offered testCase = CardHoverAction.NAVIGATE_TO_TEST_CASE.offer(p, dto);

        final @NotNull JBPanel<?> actionsPanel = AbstractDetails.row(0);

        final @NotNull AutomationState automation = Services.getInstance(p, AutomationState.class);
        automation.read(p, List.of(dto), () -> ViewToolWindowFactory.panel(p).ifPresent(ViewPanel::refreshCurrentView));

        final @NotNull Automated state = automation.of(dto.getId());

        actionsPanel.add(HoverButton.of(p, navigate, state.getIcon(), state.getLabel(), () -> navigate.action().execute(p, dto)));
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(STRUT_WIDTH)));
        actionsPanel.add(HoverButton.of(p, run, run.action().getIcon(), run.action().getTooltip(), () -> run.action().execute(p, dto)));
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(STRUT_WIDTH)));
        actionsPanel.add(HoverButton.of(p, testCase, testCase.action().getIcon(), testCase.action().getTooltip(), () -> testCase.action().execute(p, dto)));

        return actionsPanel;
    }
}
