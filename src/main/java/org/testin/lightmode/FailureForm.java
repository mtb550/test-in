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
import org.testin.testrun.create.FailureFields;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RowStripe;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * What a tester writes down when a case fails, inside the light mode window
 * (#13).
 * <p>
 * <b>Not a dialog.</b> {@code FailedResultDialog} asks for the same four things
 * in the run editor and is the right answer there, but it is modal and owned by
 * the IDE frame - opening it from here would raise IntelliJ over the window and
 * put the tester back exactly where light mode exists to keep them out of, on
 * the one verdict that needs them to stay. So the fields come from
 * {@link FailureFields}, which both surfaces share, and this only lays them out.
 * <p>
 * It takes the place of the case's details rather than being added below them.
 * There is one window and one box under the case: {@code Ctrl+D} fills it with
 * steps and tags, and a failure fills it with these four instead. Nothing is
 * added to the window and nothing is taken away.
 * <p>
 * <b>The two fields are wells, not lines on the window.</b> A dialog gets that
 * for free: its own background is the theme's panel color and a text field's is
 * the theme's field color, and the pair is what makes an input look like
 * somewhere to type. This window is painted as frame decoration instead, which
 * in some themes sits close enough to the field color that the two fields
 * disappeared into it - a caret blinking on a flat surface with no edge. They
 * take {@link RowStripe#odd()} here - the gray the grid and the card list
 * already draw every other row in, so the window borrows a gray the tester has
 * been looking at all along rather than introducing one.
 * <p>
 * <b>It zooms with the case.</b> The wheel exists so a tester can read the
 * window from where they are sitting, and a form they then have to lean in to
 * type into would have moved the problem rather than solved it. The fields are
 * framework components with fonts of their own, so the size each was built at
 * is taken once and every zoom is measured from that - derived from whatever is
 * on screen instead, a second wheel click would compound the first.
 */
class FailureForm extends JBPanel<FailureForm> {

    private final @NotNull FailureFields fields;
    private final @NotNull TestRunItems runItem;
    private final @NotNull Project p;
    private final @NotNull Path runPath;

    /**
     * The size each part of the form was built at, which is what a zoom
     * multiplies.
     */
    private final @NotNull Map<Component, Font> baseFonts = new HashMap<>();

    /**
     * How much smaller a placeholder is than the value it stands in for. A hint
     * about an empty field is not the field's content, and at this window's font
     * size - the framework's input font, then the wheel on top of it - one set
     * at the same size read as text somebody had already typed.
     */
    private static final float PLACEHOLDER_SCALE = 0.7f;

    /**
     * UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-202.
     * <p>
     * {@code resized} runs when a screenshot is pasted or taken out: the window
     * is as tall as what it holds, and without being told it kept the height the
     * form opened at, so a pasted thumbnail's lower half - its x included - sat
     * below the window's edge.
     */
    FailureForm(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunItems runItem, final float zoom, final @NotNull Runnable resized) {
        this.p = p;
        this.runPath = runPath;
        this.runItem = runItem;
        this.fields = new FailureFields(p, runPath, runItem);
        fields.onScreenshotsChanged(resized);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(JBUI.Borders.emptyTop(14));

        for (final ComponentDialogBase<?> component : fields.components()) {
            final @NotNull JComponent panel = component.getComponent().getPanel();
            panel.setAlignmentX(LEFT_ALIGNMENT);
            add(panel);
        }

        remember(this);
        setZoom(zoom);
    }

    /**
     * Draws the whole form at the window's zoom, every part from the size it was
     * built at.
     */
    void setZoom(final float zoom) {
        baseFonts.forEach((component, base) -> {
            final @NotNull Font scaled = CaseFont.zoomed(base, zoom);
            component.setFont(scaled);

            // Re-derived on every zoom rather than set once: empty text keeps
            // whatever font it was last given, so a placeholder set at the size
            // the form was built at would stay there while the field grew.
            if (component instanceof ComponentWithEmptyText hinted)
                hinted.getEmptyText().setFont(CaseFont.zoomed(scaled, PLACEHOLDER_SCALE));
        });
    }

    /**
     * Every component under this one, and the font it arrived with.
     * <p>
     * Walked rather than listed, because the four fields are framework
     * components and what they are made of is theirs - a label, an input, a row
     * of radio buttons - and a list here would go stale the first time one of
     * them gained a part.
     */
    private void remember(final @NotNull Container parent) {
        for (final Component child : parent.getComponents()) {
            if (child.getFont() != null) baseFonts.put(child, child.getFont());

            // Asked of the component rather than of our own four field types:
            // what a framework field is made of is the framework's. The two
            // kinds of text it builds are a Swing text component and, for a
            // spell-checked field, an EditorTextField, whose editor does not
            // exist until the form is shown (#314).
            if (child instanceof JTextComponent || child instanceof EditorTextField) child.setBackground(RowStripe.odd());

            if (child instanceof Container container) remember(container);
        }
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-EDITOR-PANEL-219.
     * <p>
     * Records what was typed, and the screenshots, on the run as the indexer
     * holds it - the path the failure dialog takes - rather than on the editor's
     * own row, where a sync could leave it written nowhere (#312 A25, #313).
     * Only ever called by a save: Escape leaves the case exactly as it found it.
     */
    void save() {
        Services.getInstance(p, RunStatusService.class).recordFailureDetails(p, runPath, runItem.getId(), fields);
    }

    /**
     * Puts the caret in the first field, so the tester can start typing without
     * reaching for the mouse - which is the whole reason they are in this window.
     */
    void focusFirstField() {
        fields.firstField().getFocusComponent().requestFocusInWindow();
    }
}
