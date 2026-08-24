package org.testin.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.ExplorerTree;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.EditorUtil;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Takes the tester to what they picked (#29).
 * <p>
 * Three things, in this order: the Testin tool window comes up - opened if it
 * was hidden, built if it was never opened - the tree expands to the node and
 * selects it, and the node's editor opens. A hit carrying a test case lands on
 * that case inside the editor.
 * <p>
 * The only thing that differs between a node and a case is where the keyboard
 * ends up, and it is the same question either way: a node is somewhere to be, so
 * the tree keeps the focus and the arrow keys move from it; a case is somewhere
 * to work, so its editor takes the focus and the tree only shows where you are.
 * <p>
 * Nothing reads a file. The node came out of the indexer with its path already
 * on it, and that path is what the tree matches and what the editor opens.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoTo {

    /**
     * The tool window the project tree lives in, as {@code plugin.xml} names it.
     */
    private static final @NotNull String TREE = "testin.tree";

    /**
     * Whether opening the tool window should take the keyboard with it. Named,
     * because {@code true} at a call site says nothing about what is true.
     */
    private static final boolean WITH_FOCUS = true;
    private static final boolean WITHOUT_FOCUS = false;

    public static void the(final @NotNull Project p, final @NotNull Hit hit) {
        Logger.info("Going to " + hit.name() + " in " + hit.where());

        hit.testCase().ifPresentOrElse(tc -> toCase(p, hit, tc), () -> toNode(p, hit));
    }

    /**
     * A case is somewhere to work. The tree shows where it is and the editor
     * takes the keyboard, on the case itself.
     */
    private static void toCase(final @NotNull Project p, final @NotNull Hit hit, final @NotNull TestCaseDto tc) {
        showTree(p, WITHOUT_FOCUS, tree -> tree.reveal(hit.node().getPath()));

        Services.getInstance(p, EditorUtil.class).openAndSelect(p, hit.node(), tc);
    }

    /**
     * A node is somewhere to be. The tree keeps the keyboard so the arrow keys
     * move from it, and the editor opens behind that.
     * <p>
     * A node that holds other nodes - a package, the Test Cases folder - has no
     * editor to open, and the open refuses it rather than guessing. It is still
     * worth picking: the tree went there, which is what going to a package
     * means.
     */
    private static void toNode(final @NotNull Project p, final @NotNull Hit hit) {
        showTree(p, WITH_FOCUS, tree -> tree.reveal(hit.node().getPath(), tree::focus));

        Services.getInstance(p, EditorUtil.class).open(p, hit.node());
    }

    /**
     * Brings the Testin tool window up, opening it if it was hidden and building
     * it if it was never opened, and hands the tree to the caller once it is
     * there.
     * <p>
     * Being taken somewhere you cannot see is not being taken there. The tool
     * window is closed more often than not - a tester works in the editor - so
     * this is most of what "go to" means for a node.
     * <p>
     * Building the panel here is right where refreshing in the background would
     * not be (#77): the tester asked to be taken to a node, so a panel to show
     * it in is the answer rather than a side effect.
     *
     * @param withFocus whether the tool window takes the keyboard as it opens.
     *                  False when an editor is about to want it
     */
    private static void showTree(final @NotNull Project p, final boolean withFocus,
                                 final @NotNull Consumer<ExplorerTree> onShown) {
        Optional.ofNullable(ToolWindowManager.getInstance(p).getToolWindow(TREE))
                .ifPresentOrElse(
                        tw -> tw.activate(() -> onShown.accept(
                                Services.getInstance(p, ExplorerPanel.class).getProjectTree()), withFocus),
                        () -> Logger.warn("The Testin tool window is not registered, so there is no tree to reveal in"));
    }
}
