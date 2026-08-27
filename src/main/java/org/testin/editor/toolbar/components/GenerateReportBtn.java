package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.report.GenerateReportAction;
import org.testin.util.Shortcuts;

public class GenerateReportBtn extends AbstractButton implements ToolbarItem {

    public GenerateReportBtn(final @NotNull Project p, final @NotNull RunEditor editor) {
        super("Generate Test Summary Report", AllIcons.ToolbarDecorator.Export, Shortcuts.GenerateReport);

        // The run this toolbar belongs to, not whatever the explorer tree happens
        // to have selected. Reading the tree made the button report on a different
        // run than the one on screen, and do nothing at all whenever the tree was
        // on another node or its panel was never opened - which is the default.
        // No cast to fail. It used to do nothing at all when the cast did not
        // hold - no error, no log line - and the cast could not fail, because
        // this button is on the run toolbar and no other.
        addActionListener(e -> new GenerateReportAction(p, editor).execute());
    }
}
