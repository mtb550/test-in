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

package org.testin.testcase.create;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.DialogComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006.
 * <p>
 * The test case form, as one framework component: the sections one under the
 * other, in a scroll pane that stays out of the way until they no longer fit.
 * <p>
 * One component rather than one per section. A section opens on its own key,
 * grows as the tester adds a step, and can be grayed while another is edited -
 * none of which a declared list of framework components does - so the form is
 * what the framework holds, and the sections stay what they are inside it. The
 * two dialogs used to build this by hand, each with its own popup, its own
 * status bar and its own sizing, and were the one dialog family outside the
 * framework (#66, finding 234).
 */
public final class TestCaseForm implements DialogComponent {

    private final @NotNull JBPanel<?> sections = new JBPanel<>();
    private final @NotNull JBPanel<?> panel;
    private final @NotNull JComponent focus;

    /**
     * @param focus                  what has the keyboard when the dialog opens
     * @param scrollsOnlyWhenOverflowing true for the form that grows as the
     *                               tester opens sections, whose scroll bar
     *                               would otherwise flash on while it grows
     */
    public TestCaseForm(final @NotNull JComponent focus, final boolean scrollsOnlyWhenOverflowing) {
        this.focus = focus;

        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBorder(JBUI.Borders.empty(12));

        final @NotNull JBPanel<?> anchor = new JBPanel<>(new BorderLayout());
        anchor.setOpaque(false);
        anchor.add(sections, BorderLayout.NORTH);

        final @NotNull JBScrollPane scroll = new JBScrollPane(anchor);
        scroll.setBorder(JBUI.Borders.empty());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        if (scrollsOnlyWhenOverflowing) {
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scroll.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final @NotNull ComponentEvent e) {
                    scroll.setVerticalScrollBarPolicy(anchor.getPreferredSize().height > scroll.getViewport().getHeight()
                            ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                            : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                }
            });
        } else {
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

        // Half the screen wide at least, so a description is read on one line,
        // and never taller than most of it, so the strip stays on screen.
        panel = new JBPanel<>(new BorderLayout()) {
            @Override
            public @NotNull Dimension getPreferredSize() {
                final @NotNull Dimension pref = super.getPreferredSize();
                final @NotNull Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                pref.width = Math.max(pref.width, screen.width / 2);
                pref.height = Math.min(pref.height, (int) (screen.height * 0.85));
                return pref;
            }
        };
        panel.setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
    }

    /**
     * A new place for one section, at the bottom of the form.
     */
    public @NotNull JBPanel<?> newSlot() {
        final @NotNull JBPanel<?> slot = new JBPanel<>(new BorderLayout());
        slot.setOpaque(false);
        sections.add(slot);
        return slot;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return focus;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Enter is the dialog's, bound on the form so it reaches past the
        // editors inside it: see TestCaseBaseDialog.registerShortcut.
    }

    @Override
    public boolean acceptsDialogKeys() {
        // The form binds its own keys, section by section.
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
