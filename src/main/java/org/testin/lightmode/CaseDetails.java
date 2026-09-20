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

package org.testin.lightmode;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.ui.framework.Prose;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.Display;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

class CaseDetails extends JBPanel<CaseDetails> {
    static final int GAP = 10;

    private @NotNull Optional<TestCaseDto> shown = Optional.empty();

    private final @NotNull Project p;

    private float zoom = 1.0f;

    private boolean cutOff = false;

    CaseDetails(final @NotNull Project p) {
        super(new GridBagLayout());
        setOpaque(false);

        this.p = p;
    }

    void show(final @NotNull TestCaseDto tc) {
        shown = Optional.of(tc);

        render();
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217
    void setCutOff(final boolean truncated) {
        if (cutOff == truncated) return;

        cutOff = truncated;
        render();
    }

    void setZoom(final float zoom) {
        if (this.zoom == zoom) return;

        this.zoom = zoom;
        render();
    }

    private void render() {
        removeAll();

        shown.ifPresent(this::rows);
    }

    private void rows(final @NotNull TestCaseDto tc) {
        addTags(tc);

        addRow(CreateTestCaseFields.STEPS.getIcon(), Display.numberedSteps(tc.getSteps()));

        addRow(CreateTestCaseFields.TEST_DATA.getIcon(), tc.getTestData());
        addRow(CreateTestCaseFields.PRE_CONDITIONS.getIcon(), TestEditorAttributes.PRE_CONDITIONS.displayValue(tc));

        if (cutOff) addCutOffNotice();
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217
    private void addCutOffNotice() {
        final @NotNull JBLabel notice = new JBLabel(Bundle.message("light.cut.off"));
        notice.setFont(CaseFont.zoomed(CaseFont.label(), zoom));
        notice.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        addRow(EmptyIcon.ICON_16, notice);
    }

    private void addRow(final @NotNull Icon icon, final @NotNull String value) {
        if (value.isBlank()) return;

        addRow(icon, prose(value));
    }

    private void addRow(final @NotNull Icon icon, final @NotNull JComponent value) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, GAP, 0);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(new JBLabel(icon), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(0, GAP, GAP, 0);
        add(value, gbc);
    }

    private void addTags(final @NotNull TestCaseDto tc) {
        final @NotNull List<Badges.Badge> badges = Badges.caseBadges(tc);

        if (badges.isEmpty()) return;

        final @NotNull JBPanel<?> chips = new JBPanel<>(new HorizontalLayout(5));
        chips.setOpaque(false);
        Badges.showBadges(chips, badges);

        addRow(EmptyIcon.ICON_16, chips);
    }

    private @NotNull JTextArea prose(final @NotNull String text) {
        final @NotNull JTextArea area = Prose.of(CaseFont.zoomed(CaseFont.body(), zoom), JBUI.CurrentTheme.Label.foreground());
        area.setText(text);

        return area;
    }
}
