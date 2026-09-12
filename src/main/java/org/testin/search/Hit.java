package org.testin.search;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.Bundle;

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
 * @param kind     what this is, in one word beside the name - a test set, a test
 *                 case, a test run, a package. One query answers with all four,
 *                 and a 16-pixel icon was the only thing telling them apart
 *                 (#66, finding 51)
 * @param node     what the tree expands to and what the editor opens. For a test
 *                 case this is the test set it lives in, because a case is not a
 *                 node of its own
 * @param testCase the row to land on, and empty for a node - which is a node
 *                 selected and nothing more
 */
public record Hit(@NotNull Icon icon, @NotNull String name, @NotNull String kind, @NotNull String where, @NotNull DirectoryDto node, @NotNull Optional<TestCaseDto> testCase) {

    /**
     * A test case, shown under the test set that holds it.
     */
    public static @NotNull Hit of(final @NotNull TestCaseDto tc) {
        return new Hit(AllIcons.Nodes.Class, tc.getDescription(), Bundle.message("caption.test.case"),
                where(tc.getParent()), tc.getParent(), Optional.of(tc));
    }

    /**
     * A node, shown under whatever leads to it, and drawn with the icon its own
     * type declares - so a test set, a package and a run look in the search
     * exactly as they look in the tree.
     * <p>
     * Its kind is the type's own description, which is where that word already
     * lives: the tree and the Details popup read the same one, so the search
     * cannot come to call a test set something else.
     */
    public static @NotNull Hit of(final @NotNull DirectoryDto node) {
        return new Hit(node.getType().getIcon(), node.getName(), node.getType().getDescription(),
                where(node), node, Optional.empty());
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
