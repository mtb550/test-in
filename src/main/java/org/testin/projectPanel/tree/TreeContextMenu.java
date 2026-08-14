package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.ShowNodeDetailsAction;
import org.testin.clipboard.*;
import org.testin.enums.ProjectStatus;
import org.testin.generateReport.GenerateReportAction;
import org.testin.git.SyncActionAction;
import org.testin.git.ViewPendingCommitsAction;
import org.testin.importExport.exports.ExportAction;
import org.testin.importExport.imports.ImportAction;
import org.testin.nodeCreator.CreateTreeNodeAction;
import org.testin.open.OpenAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.projectPanel.ProjectPanel;
import org.testin.remove.RemoveAction;
import org.testin.rename.RenameAction;
import org.testin.run.RunTestSetAction;
import org.testin.services.Services;
import org.testin.testProject.UpdateTestProjectStatusAction;
import org.testin.testRun.SetTestRunStatusAction;
import org.testin.util.OptionalPlugin;
import org.testin.util.Tools;

import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    private final @NotNull Project p;

    public TreeContextMenu(final @NotNull Project p, final @NotNull ProjectPanel pp, final @NotNull SimpleTree tree) {
        super("Tree Popup Menu", true);
        this.p = p;

        add(new OpenAction(p, tree));
        add(new CreateTreeNodeAction(p, tree));

        addSeparator();

        add(Services.getInstance(p, Tools.class).createSubGroup("Actions", AllIcons.Actions.Edit,
                List.of(
                        new UpdateTestProjectStatusAction(p, tree, ProjectStatus.ACTIVE),
                        new UpdateTestProjectStatusAction(p, tree, ProjectStatus.INACTIVE),
                        new UpdateTestProjectStatusAction(p, tree, ProjectStatus.ARCHIVED),
                        new UndoNodeAction(p, tree),
                        new RedoNodeAction(p, tree),
                        new RemoveAction(p, tree, pp),
                        new RenameAction(p, pp, tree),
                        new CopyNodeAction(tree),
                        new CutNodeAction(tree),
                        new PasteNodeAction(p, tree))
        ));

        addSeparator();

        add(new RunTestSetAction(p, tree));

        addSeparator();

        add(new ExportAction(p, tree));

        add(new ImportAction(p, tree));

        if (OptionalPlugin.GIT.isAvailable()) {
            addSeparator();
            add(new SyncActionAction(p, tree, pp));
            add(new ViewPendingCommitsAction(p, tree));
        }

        addSeparator();
        add(new SetTestRunStatusAction(p, tree));
        addSeparator();

        add(new GenerateReportAction(p, tree));

        add(new ShowNodeDetailsAction(p, tree));

    }

    public void registerShortcuts(final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        new EscapeAction(p, tree, transferHandler);
        new OpenContextMenuAction(p, tree, this);

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}