package org.testin.explorer.tree;

import org.testin.ui.ActionsMenu;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.testin.actions.Declared;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import org.testin.logger.Logger;
import org.testin.sftp.SyncWithSftpAction;
import org.testin.EscapeAction;
import org.testin.explorer.TreePanel;
import org.testin.git.SyncActionAction;
import org.testin.git.ViewPendingCommitsAction;
import org.testin.importexport.imports.ImportAction;
import org.testin.model.PackageStatus;
import org.testin.model.ProjectStatus;
import org.testin.model.TestSetStatus;
import org.testin.open.OpenContextMenuAction;
import org.testin.remove.RemoveAction;
import org.testin.report.GenerateReportAction;
import org.testin.testproject.UpdateTestProjectStatusAction;
import org.testin.testrun.EditTestRunAction;
import org.testin.testrun.SetTestRunStatusAction;
import org.testin.testset.UpdateTestSetStatusAction;
import org.testin.undo.UndoAction;
import org.testin.undo.UndoDirection;
import org.testin.undo.UndoScope;
import org.testin.services.OptionalPlugin;

import java.util.ArrayList;
import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    private final @NotNull Project p;

    public TreeContextMenu(final @NotNull Project p, final @NotNull TreePanel tp, final @NotNull SimpleTree tree) {
        super("Tree Popup Menu", true);
        this.p = p;

        add(Declared.action("Testin.Open"));
        add(Declared.action("Testin.CreateNode"));

        addSeparator();

        // Every status each kind has, rather than the seven that were listed here.
        // A status is a constant on its enum and this menu is the only place a
        // tester reaches it, so one added there and not here would be a status
        // nothing could ever set - and nothing would have said so (#175, C9).
        final @NotNull List<DumbAwareAction> statusActions = new ArrayList<>();
        for (final ProjectStatus status : ProjectStatus.values()) statusActions.add(new UpdateTestProjectStatusAction(p, tree, status));
        for (final TestSetStatus status : TestSetStatus.values()) statusActions.add(new UpdateTestSetStatusAction(p, tree, status));
        for (final PackageStatus status : PackageStatus.values()) statusActions.add(new UpdatePackageStatusAction(p, tree, status));

        add(actionsSubMenu(statusActions, List.of(
                        new UndoAction(p, tree, UndoScope.TREE, UndoDirection.UNDO),
                        new UndoAction(p, tree, UndoScope.TREE, UndoDirection.REDO),
                        Declared.action("Testin.ReCreateTestRun"),
                        new RemoveAction(p, tree, tp),
                        Declared.action("Testin.Rename"),
                        Declared.action("Testin.OrderNode"),
                        Declared.action("Testin.CopyNode"),
                        Declared.action("Testin.CutNode"),
                        Declared.action("Testin.PasteNode"))));

        if (OptionalPlugin.TESTNG.isAvailable()) {
            addSeparator();
            add(Declared.action("Testin.RunTests"));
        }

        addSeparator();

        add(Declared.action("Testin.Export"));

        add(new ImportAction(p, tree));

        // Added in every IDE, and grayed with the reason when Git is missing.
        // Leaving them out gave the menu a different shape in two IDEs with
        // nothing to say why, while Sync With SFTP below was added in both
        // (#273).
        addSeparator();
        add(new SyncActionAction(p, tree));
        add(new ViewPendingCommitsAction(p, tree));

        addSeparator();
        add(new SyncWithSftpAction(p, tree, tp));

        addSeparator();
        add(new EditTestRunAction(p, tp, tree));
        add(new SetTestRunStatusAction(p, tree));
        addSeparator();

        add(new GenerateReportAction(p, tree));

        add(Declared.action("Testin.ShowNodeDetails"));

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
    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-001.
     * <p>
     * An action the platform owns, by the id {@code plugin.xml} gives it (#119).
     * <p>
     * A declared action is one instance for the whole IDE, built by the
     * platform, so it is fetched rather than constructed - and fetching it is
     * what keeps the menu and the keymap showing the same thing. An id that
     * names nothing is a wiring mistake rather than a state, so it is said once
     * here instead of returning something that quietly does nothing.
     */
    /**
     * UC-TREE-PANEL-001.
     * <p>
     * Puts a declared action's key on one component rather than in the keymap.
     * <p>
     * For the keys that are a surface's gesture and not a command: ENTER opens
     * what a tree has selected, and a global ENTER would fire in every editor in
     * the IDE. The action is still declared - Find Action offers it, the Keymap
     * lists it with no default - and there is still one action behind the menu
     * entry and the key (#119).
     */
    public static void bindToTree(final @NotNull String id, final @NotNull Shortcuts shortcut, final @NotNull JComponent tree) {
        declared(id).registerCustomShortcutSet(shortcut.getCustomShortcut(), tree);
    }

    private static @NotNull AnAction declared(final @NotNull String id) {
        final @Nullable AnAction action = ActionManager.getInstance().getAction(id);

        if (action == null) {
            Logger.error("No action is registered as '" + id + "', so the menu is missing an entry");
            throw new IllegalStateException("No action is registered as '" + id + "'");
        }

        return action;
    }

    /**
     * The Actions submenu: every status each kind can be set to, then the rest.
     * <p>
     * Two lists rather than one because the first is generated from enums and the
     * second is written out - and a generated group joined to a literal one reads
     * better than either a stream of both or a list nobody can tell apart.
     */
    private static @NotNull DefaultActionGroup actionsSubMenu(final @NotNull List<? extends AnAction> statusActions, final @NotNull List<? extends AnAction> rest) {
        final @NotNull DefaultActionGroup group = ActionsMenu.group();
        statusActions.forEach(group::add);
        rest.forEach(group::add);
        return group;
    }

}