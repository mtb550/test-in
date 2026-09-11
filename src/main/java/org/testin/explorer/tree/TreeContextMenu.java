package org.testin.explorer.tree;

import org.testin.ui.ActionsMenu;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.testin.actions.Declared;
import org.jetbrains.annotations.NotNull;

import org.testin.actions.EscapeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.report.GenerateReportAction;
import org.testin.testrun.SetTestRunStatusAction;
import org.testin.undo.UndoAction;
import org.testin.undo.UndoDirection;
import org.testin.undo.UndoScope;
import org.testin.services.OptionalPlugin;

import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    private final @NotNull Project p;

    public TreeContextMenu(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Tree Popup Menu", true);
        this.p = p;

        add(Declared.forMenu("Testin.Open"));
        add(Declared.forMenu("Testin.CreateNode"));

        addSeparator();

        // Every status the selected node has, rather than the seven that were
        // listed here. A status is a constant on its enum and this menu is the
        // only place a tester reaches it, so one added there and not here would
        // be a status nothing could ever set - and nothing would have said so
        // (#175, C9). One declared group generates them, from the node rather
        // than from a group per kind of node (#119, #110).
        add(actionsSubMenu(List.of(
                        Declared.forMenu("Testin.UpdateStatus")), List.of(
                        new UndoAction(p, tree, UndoScope.TREE, UndoDirection.UNDO),
                        new UndoAction(p, tree, UndoScope.TREE, UndoDirection.REDO),
                        Declared.forMenu("Testin.ReCreateTestRun"),
                        Declared.forMenu("Testin.RemoveNode"),
                        Declared.forMenu("Testin.Rename"),
                        Declared.forMenu("Testin.OrderNode"),
                        Declared.forMenu("Testin.CopyNode"),
                        Declared.forMenu("Testin.CutNode"),
                        Declared.forMenu("Testin.PasteNode"))));

        if (OptionalPlugin.TESTNG.isAvailable()) {
            addSeparator();
            add(Declared.forMenu("Testin.RunTests"));
        }

        addSeparator();

        add(Declared.forMenu("Testin.Export"));

        add(Declared.forMenu("Testin.Import"));

        // Added in every IDE, and grayed with the reason when Git is missing.
        // Leaving them out gave the menu a different shape in two IDEs with
        // nothing to say why, while Sync With SFTP below was added in both
        // (#273).
        addSeparator();
        add(Declared.forMenu("Testin.SyncWithRemote"));
        add(Declared.forMenu("Testin.ViewPendingCommits"));

        addSeparator();
        add(Declared.forMenu("Testin.SyncWithSftp"));

        addSeparator();
        add(Declared.forMenu("Testin.EditTestRun"));
        add(new SetTestRunStatusAction(p, tree));
        addSeparator();

        add(new GenerateReportAction(p, tree));

        add(Declared.forMenu("Testin.ShowNodeDetails"));

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
     * The Actions submenu: every status each kind can be set to, then the rest.
     * <p>
     * Two lists rather than one because the first is the three declared status
     * groups, each generating its entries from an enum, and the second is
     * written out - and a generated group joined to a literal one reads better
     * than either a stream of both or a list nobody can tell apart.
     */
    private static @NotNull DefaultActionGroup actionsSubMenu(final @NotNull List<? extends AnAction> statusGroups, final @NotNull List<? extends AnAction> rest) {
        final @NotNull DefaultActionGroup group = ActionsMenu.group();
        statusGroups.forEach(group::add);
        rest.forEach(group::add);
        return group;
    }

}