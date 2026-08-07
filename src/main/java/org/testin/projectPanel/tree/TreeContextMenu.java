package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.*;
import org.testin.clipboard.*;
import org.testin.crud.RemoveAction;
import org.testin.enums.ProjectStatus;
import org.testin.generateReport.GenerateReportAction;
import org.testin.importExport.exports.ExportAction;
import org.testin.importExport.imports.ImportAction;
import org.testin.nodeCreator.CreateTreeNodeAction;
import org.testin.projectPanel.ProjectPanel;
import org.testin.run.RunTestSetAction;
import org.testin.testProject.UpdateTestProjectStatusAction;
import org.testin.testRun.SetTestRunStatusAction;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    final @NotNull Project project;

    public TreeContextMenu(final @NotNull Project p, final @NotNull ProjectPanel projectPanel, final @NotNull SimpleTree tree) {
        super("Tree Popup Menu", true);
        this.project = p;

        add(new OpenActionAction(tree));
        add(new CreateTreeNodeAction(projectPanel, tree));

        addSeparator();

        add(Services.getInstance(p, Tools.class).createSubGroup("Actions", AllIcons.Actions.Edit,
                List.of(
                        new UpdateTestProjectStatusAction(tree, ProjectStatus.ACTIVE),
                        new UpdateTestProjectStatusAction(tree, ProjectStatus.INACTIVE),
                        new UpdateTestProjectStatusAction(tree, ProjectStatus.ARCHIVED),
                        new UndoNodeAction(tree),
                        new RedoNodeAction(tree),
                        new RemoveAction(tree, projectPanel),
                        new RenameAction(projectPanel, tree),
                        new CopyNodeAction(tree),
                        new CutNodeAction(tree),
                        new PasteNodeAction(tree))
        ));

        addSeparator();

        add(new RunTestSetAction(tree));

        addSeparator();

        add(new ExportAction(tree));

        add(new ImportAction(tree));

        addSeparator();

        add(new SyncActionAction(tree, projectPanel));
        add(new ViewPendingCommitsAction(tree));

        addSeparator();
        add(new SetTestRunStatusAction(tree));
        addSeparator();

        add(new GenerateReportAction(tree));

        add(new ShowNodeDetailsAction(tree));

    }

    public void registerShortcuts(final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        new EscapeAction(tree, transferHandler);
        new OpenContextMenuAction(tree, this);

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}