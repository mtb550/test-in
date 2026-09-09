package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunStatus;
import org.testin.report.GenerateReportAction;
import org.testin.util.Shortcuts;

public class GenerateReportBtn extends AbstractIconButton implements ToolbarItem {

    /**
     * The run this button reports on, kept so its status can be asked again
     * every time the run changes - which is what decides whether the button
     * works.
     */
    private final @NotNull RunEditor editor;

    // UC-REPORT-001
    public GenerateReportBtn(final @NotNull Project p, final @NotNull RunEditor editor) {
        super("Generate Test Summary Report", AllIcons.ToolbarDecorator.Export, Shortcuts.GenerateReport);
        this.editor = editor;

        // The run this toolbar belongs to, not whatever the explorer tree happens
        // to have selected. Reading the tree made the button report on a different
        // run than the one on screen, and do nothing at all whenever the tree was
        // on another node or its panel was never opened - which is the default.
        // No cast to fail. It used to do nothing at all when the cast did not
        // hold - no error, no log line - and the cast could not fail, because
        // this button is on the run toolbar and no other.
        addActionListener(e -> new GenerateReportAction(p, editor).execute());

        updateEnabledState();
    }

    /**
     * UC-REPORT-001, Rule-REPORT-016.
     * <p>
     * Gray while the run is still going, with the reason in the tooltip - shown
     * and disabled, never hidden (#253).
     */
    public void updateEnabledState() {
        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();

        setEnabled(status.isReportable());
        setToolTipText(status.isReportable() ? "Generate Test Summary Report"
                : "A report is written once the run has stopped — it is " + status.getLabel());
    }
}
