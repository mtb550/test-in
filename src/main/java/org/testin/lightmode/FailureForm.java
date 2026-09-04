package org.testin.lightmode;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.testrun.create.FailureFields;
import org.testin.ui.framework.ComponentDialogBase;

import javax.swing.*;

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
 */
class FailureForm extends JBPanel<FailureForm> {

    private final @NotNull FailureFields fields;
    private final @NotNull TestRunItems runItem;

    FailureForm(final @NotNull TestRunItems runItem) {
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
