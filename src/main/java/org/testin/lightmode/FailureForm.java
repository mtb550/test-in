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
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.services.Services;
import org.testin.testrun.RunStatusService;
import org.testin.testrun.failure.FailureFields;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RowStripe;
import org.testin.util.Fonts;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class FailureForm extends JBPanel<FailureForm> {
    private final @NotNull FailureFields fields;
    private final @NotNull TestRunItems runItem;
    private final @NotNull Project p;
    private final @NotNull Path runPath;
    private final @NotNull Map<Component, Font> baseFonts = new HashMap<>();

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-202
    FailureForm(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunItems runItem, final float zoom, final @NotNull Runnable resized) {
        this.p = p;
        this.runPath = runPath;
        this.runItem = runItem;
        this.fields = new FailureFields(p, runPath, runItem);
        fields.onResized(resized);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(JBUI.Borders.emptyTop(14));

        for (final ComponentDialogBase<?> component : fields.components()) {
            final @NotNull JComponent panel = component.getComponent().getPanel();
            panel.setAlignmentX(LEFT_ALIGNMENT);
            add(panel);

            // Rule-EDITOR-PANEL-219
            component.getComponent().installedOn(this);
        }

        remember(this);
        setZoom(zoom);
    }

    void setZoom(final float zoom) {
        baseFonts.forEach((component, base) -> {
            component.setFont(Fonts.zoomed(base, zoom));

            if (component instanceof ComponentWithEmptyText hinted)
                hinted.getEmptyText().setFont(Fonts.zoomed(Fonts.placeholder(), zoom));
        });
    }

    private void remember(final @NotNull Container parent) {
        for (final Component child : parent.getComponents()) {
            if (child.getFont() != null) baseFonts.put(child, child.getFont());

            if (child instanceof JTextComponent || child instanceof EditorTextField)
                child.setBackground(RowStripe.odd());

            if (child instanceof Container container) remember(container);
        }
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-EDITOR-PANEL-219
    boolean save() {
        return Services.getInstance(p, RunStatusService.class).recordFailureDetails(p, runPath, runItem.getId(), fields);
    }

    void focusFirstField() {
        fields.actualResult().getFocusComponent().requestFocusInWindow();
    }
}
