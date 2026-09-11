package org.testin.testcase.create;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.statusbar.StatusBarBase;
import org.testin.model.StatusBarItem;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestCaseDialogKey;

import java.awt.BorderLayout;

/**
 * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-199, Rule-EDITOR-PANEL-200.
 * <p>
 * The hint strips at the bottom of a test case dialog: the keys of the field
 * the tester is in, above the two that mean the same thing everywhere.
 * <p>
 * One strip until now, rebuilt from scratch on every focus change. Save and
 * Cancel were rebuilt with it, so the one part of the dialog that never changes
 * was redrawn every time the tester tabbed from Description to Steps to
 * Priority - the most stable thing in the dialog behaving like the least
 * stable (#56).
 * <p>
 * The shared strip is built once and never touched again. Only the strip above
 * it is redrawn, and it carries no keyboard icon: two of them on two stacked
 * rows read as two unrelated bars rather than one hint area.
 * <p>
 * A section with no keys of its own hides its strip rather than drawing an
 * empty one - a tinted row with nothing on it reads as a broken row, not as an
 * absent one.
 * <p>
 * Both dialogs get the pair by asking for the panel, which is what they already
 * did, so neither assembles anything itself.
 */
public final class StatusBarSection {

    /**
     * The two keys every section shares, in the order a tester reads them.
     */
    private static final StatusBarItem @NotNull [] SHARED =
            {TestCaseDialogKey.SAVE, TestCaseDialogKey.CANCEL};

    private final @NotNull StatusBarBase section = new StatusBarBase(new StatusBarItem[0], StatusBarBase.WITHOUT_ICON);
    private final @NotNull StatusBarBase shared = new StatusBarBase(SHARED);
    private final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());

    public StatusBarSection() {
        panel.setOpaque(false);
        panel.add(section.getPanel(), BorderLayout.NORTH);
        panel.add(shared.getPanel(), BorderLayout.SOUTH);

        // The dialog opens on the description, so its keys are what the strip
        // starts on - the same section the focus listener would report first.
        updateItems(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());
    }

    /**
     * Rule-EDITOR-PANEL-199, Rule-EDITOR-PANEL-200.
     * <p>
     * The focused section's own keys. The shared strip below is not told, and
     * has nothing to be told.
     */
    public void updateItems(final StatusBarItem @NotNull [] items) {
        section.updateItems(items);
        section.setShown(items.length > 0);
    }

    public @NotNull JBPanel<?> getPanel() {
        return panel;
    }
}
