package org.testin.search;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;

import javax.swing.*;
import java.util.Optional;

/**
 * One thing the search found, and enough to go to it (#29).
 * <p>
 * A test case and a node are one shape here on purpose. Both are somewhere in
 * the tree, both open an editor, and the only difference is whether there is a
 * row to land on once it is open - so a hit carries the node to reveal and, for
 * a case, the case to select inside it. Nothing downstream asks which kind it
 * got.
 *
 * @param node     what the tree expands to and what the editor opens. For a test
 *                 case this is the test set it lives in, because a case is not a
 *                 node of its own
 * @param testCase the row to land on, and empty for a node - which is a node
 *                 selected and nothing more
 */
public record Hit(@NotNull Icon icon, @NotNull String name, @NotNull String where, @NotNull DirectoryDto node, @NotNull Optional<TestCaseDto> testCase) {

    /**
     * A test case, shown under the test set that holds it.
     */
    public static @NotNull Hit of(final @NotNull TestCaseDto tc) {
        return new Hit(AllIcons.Nodes.Class, tc.getDescription(), where(tc.getParent()),
                tc.getParent(), Optional.of(tc));
    }

    /**
     * A node, shown under whatever leads to it, and drawn with the icon its own
     * type declares - so a test set, a package and a run look in the search
     * exactly as they look in the tree.
     */
    public static @NotNull Hit of(final @NotNull DirectoryDto node) {
        return new Hit(node.getType().getIcon(), node.getName(), where(node), node, Optional.empty());
    }

    /**
     * The chain that leads to a node, as the tree reads it. Taken from the names
     * the scan already put on the node rather than from its path, so what the
     * search shows and what the tree shows are the same words.
     */
    private static @NotNull String where(final @NotNull DirectoryDto node) {
        return String.join(" > ", node.getPath2());
    }
}
