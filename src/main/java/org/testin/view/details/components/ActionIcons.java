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

import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.codegen.AutomationState;
import org.testin.editor.CardHoverAction;
import org.testin.model.Automated;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionIcons extends BaseDetails {
    final float BASE_SCALE = 1.3f;
    final float HOVER_SCALE = 1.8f;
    final int STRUT_WIDTH = 8;
    final int INSETS_TOP = 8;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 0;
    final int INSETS_RIGHT = 16;

    // UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-050, Rule-VIEW-PANEL-056, Rule-VIEW-PANEL-057
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        // The run slot draws what clicking it does, not how the last run went: a
        // passed case used to show a green tick here, which reads as a verdict
        // and is one - the verdict is a badge now, below.
        final @NotNull CardHoverAction navigate = CardHoverAction.NAVIGATE_TO_TEST_METHOD;
        final @NotNull CardHoverAction run = CardHoverAction.runSlot(p, dto);

        // Both are drawn whatever this IDE can do. An icon the IDE cannot act on
        // is gray and says which plugin it is waiting for, rather than being
        // left off the panel - a row that is sometimes there and sometimes not
        // teaches nobody what is missing (#312, A16).

        final @NotNull JBPanel<?> actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        // The panel shows one test case, so its page is one case - and the read
        // is the same fired-and-forgotten one the cards make. It answers into the
        // same service, so a case read here is already known when its card is
        // drawn, and the other way round.
        final @NotNull AutomationState automation = Services.getInstance(p, AutomationState.class);
        //
        // Drawn again when it answers, not only repainted: the icon below is
        // chosen once, as the row is built, so a repaint drew the "not read yet"
        // icon a second time over a case whose state had arrived (#312, A64).
        // The second read finds nothing new and does not call back, so this ends.
        automation.read(p, List.of(dto), () -> ViewToolWindowFactory.panel(p).ifPresent(ViewPanel::refreshCurrentView));

        final @NotNull Automated state = automation.of(dto.getId());

        actionsPanel.add(hoverIcon(navigate, p, dto, state.getIcon(), state.getLabel()));
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(STRUT_WIDTH)));
        actionsPanel.add(hoverIcon(run, p, dto, run.getIcon(), run.getTooltip()));

        return addFullWidthRow(panel, gbc, actionsPanel,
                JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT), currentRow);
    }

    /**
     * One action's icon: a label that grows on hover and does that action's work
     * on click. Sized to the hovered icon from the start, so growing it does not
     * reflow the row.
     * <p>
     * Everything it draws comes off the action itself - the icon, the tooltip,
     * the key it names, and what the click does - so this panel and the cards
     * cannot end up disagreeing about a button they both show.
     */
    private @NotNull JBLabel hoverIcon(final @NotNull CardHoverAction action, final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull Icon drawn, final @NotNull String tooltip) {
        // Gray, with the plugin it waits for as its whole tooltip, when this IDE
        // cannot act on it (#312, A16). It does not grow under the pointer and
        // the pointer stays an arrow: both are the promise that pressing does
        // something.
        final @NotNull Optional<String> whyNot = action.whyNotOffered(p);

        final @NotNull JBLabel label = new JBLabel();
        final @NotNull Icon shown = whyNot.isEmpty() ? drawn : IconLoader.getDisabledIcon(drawn);
        final @NotNull Icon base = IconUtil.scale(shown, label, BASE_SCALE);
        final @NotNull Icon hover = IconUtil.scale(shown, label, whyNot.isEmpty() ? HOVER_SCALE : BASE_SCALE);
        label.setIcon(base);
        label.setCursor(Cursor.getPredefinedCursor(whyNot.isEmpty() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        new HelpTooltip()
                .setDescription(HtmlChunk.text(whyNot.orElse(tooltip)))
                .setShortcut(whyNot.isEmpty() ? Declared.shortcutText(action.getActionId()) : "")
                .installOn(label);

        // From the hovered icon itself: scaling 16px by 1.8 gives 28.8, which the
        // icon reports as 29 and the estimate truncated to 28, clipping a pixel.
        label.setPreferredSize(new Dimension(hover.getIconWidth(), hover.getIconHeight()));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);

        label.addMouseListener(new MouseAdapter() {
            // UC-VIEW-PANEL-012, Rule-VIEW-PANEL-052
            @Override
            public void mouseEntered(final MouseEvent e) {
                label.setIcon(hover);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                label.setIcon(base);
            }

            // UC-VIEW-PANEL-012, UC-VIEW-PANEL-014
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (whyNot.isPresent()) {
                    Services.getInstance(p, Notifier.class).softRefuse(p, whyNot.orElseThrow());
                    return;
                }

                action.execute(p, dto);
            }
        });

        return label;
    }
}
