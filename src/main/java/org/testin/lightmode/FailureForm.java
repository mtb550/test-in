package org.testin.lightmode;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.testrun.create.FailureFields;
import org.testin.ui.framework.ComponentDialogBase;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
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

    /**
     * The size each part of the form was built at, which is what a zoom
     * multiplies.
     */
    private final @NotNull Map<Component, Font> baseFonts = new HashMap<>();

    FailureForm(final @NotNull TestRunItems runItem, final float zoom) {
        this.runItem = runItem;
        this.fields = new FailureFields(runItem);

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
        baseFonts.forEach((component, base) -> component.setFont(base.deriveFont(base.getSize2D() * zoom)));
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

            if (child instanceof Container container) remember(container);
        }
    }

    /**
     * Writes what was typed onto the run row. Only ever called by a save -
     * Escape leaves the case exactly as it found it.
     */
    void save() {
        fields.applyTo(runItem);
    }

    /**
     * Puts the caret in the first field, so the tester can start typing without
     * reaching for the mouse - which is the whole reason they are in this window.
     */
    void focusFirstField() {
        fields.firstField().getFocusComponent().requestFocusInWindow();
    }
}
