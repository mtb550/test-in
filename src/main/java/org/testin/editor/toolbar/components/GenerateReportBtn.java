package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.report.GenerateReportAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.services.Services;

public class GenerateReportBtn extends AbstractButton implements ToolbarItem {

    public GenerateReportBtn(final @NotNull Project p) {
        super("Export Results", AllIcons.ToolbarDecorator.Export);

        addActionListener(e -> {
            final SimpleTree tree = Services.getInstance(p, ExplorerPanel.class).getProjectTree().getMainTree();
            final GenerateReportAction action = new GenerateReportAction(p, tree);

            if (action.isAvailable()) {
                action.execute();
            }
        });
    }
}
