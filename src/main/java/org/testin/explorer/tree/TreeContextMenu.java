package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.sftp.SyncWithSftpAction;
import org.testin.EscapeAction;
import org.testin.ShowNodeDetailsAction;
import org.testin.clipboard.*;
import org.testin.creator.CreateTreeNodeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.git.SyncActionAction;
import org.testin.git.ViewPendingCommitsAction;
import org.testin.importexport.exports.ExportAction;
import org.testin.importexport.imports.ImportAction;
import org.testin.model.PackageStatus;
import org.testin.model.ProjectStatus;
import org.testin.model.TestSetStatus;
import org.testin.open.OpenAction;
import org.testin.order.OrderNodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.remove.RemoveAction;
import org.testin.rename.RenameAction;
import org.testin.report.GenerateReportAction;
import org.testin.run.RunTestSetAction;
import org.testin.testproject.UpdateTestProjectStatusAction;
import org.testin.testrun.SetTestRunStatusAction;
import org.testin.testset.UpdateTestSetStatusAction;
import org.testin.util.OptionalPlugin;

import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    private final @NotNull Project p;

    public TreeContextMenu(final @NotNull Project p, final @NotNull ExplorerPanel pp, final @NotNull SimpleTree tree) {
        super("Tree Popup Menu", true);
        this.p = p;

        add(new OpenAction(p, tree));
        add(new CreateTreeNodeAction(p, tree));

        addSeparator();

        add(actionsSubMenu(List.of(
                        new UpdateTestProjectStatusAction(p, tree, ProjectStatus.ACTIVE),
                        new UpdateTestProjectStatusAction(p, tree, ProjectStatus.INACTIVE),
                        new UpdateTestProjectStatusAction(p, tree, ProjectStatus.ARCHIVED),
                        new UpdateTestSetStatusAction(p, tree, TestSetStatus.ACTIVE),
                        new UpdateTestSetStatusAction(p, tree, TestSetStatus.DEPRECATED),
                        new UpdatePackageStatusAction(p, tree, PackageStatus.ACTIVE),
                        new UpdatePackageStatusAction(p, tree, PackageStatus.ARCHIVED),
                        new UndoNodeAction(p, tree),
                        new RedoNodeAction(p, tree),
                        new RemoveAction(p, tree, pp),
                        new RenameAction(p, pp, tree),
                        new OrderNodeAction(p, pp, tree),
                        new CopyNodeAction(tree),
                        new CutNodeAction(tree),
                        new PasteNodeAction(p, tree))
        ));

        if (OptionalPlugin.TESTNG.isAvailable()) {
            addSeparator();
            add(new RunTestSetAction(p, tree));
        }

        addSeparator();

        add(new ExportAction(p, tree));

        add(new ImportAction(p, tree));

        if (OptionalPlugin.GIT.isAvailable()) {
            addSeparator();
            add(new SyncActionAction(p, tree, pp));
            add(new ViewPendingCommitsAction(p, tree));
        }

        addSeparator();
        add(new SyncWithSftpAction(p, tree, pp));

        addSeparator();
        add(new SetTestRunStatusAction(p, tree));
        addSeparator();

        add(new GenerateReportAction(p, tree));

        add(new ShowNodeDetailsAction(p, tree));

    }

    public void registerShortcuts(final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        new EscapeAction(p, tree, transferHandler);
        new OpenContextMenuAction(tree, this);

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


    /**
     * The "Actions" submenu - everything that changes a node rather than opening
     * one. Two lines of platform setup that only this menu needs; they used to
     * live in a shared utility class where this was the one caller.
     */
    private static @NotNull DefaultActionGroup actionsSubMenu(final @NotNull List<? extends DumbAwareAction> actions) {
        final @NotNull DefaultActionGroup group = new DefaultActionGroup("Actions", true);
        group.getTemplatePresentation().setIcon(AllIcons.Actions.Edit);
        actions.forEach(group::add);
        return group;
    }

}