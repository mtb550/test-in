package org.testin.navigate;

import com.intellij.platform.navbar.backend.NavBarItem;
import com.intellij.platform.navbar.backend.NavBarItemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.List;

/**
 * How the IDE's navigation bar walks up a Testin path (#161).
 * <p>
 * The bar is handed the node the focused editor is open on - see
 * {@code UnifiedFileEditor}, which answers {@code NavBarItem.NAVBAR_ITEM_KEY} -
 * and then asks this, one step at a time, until there is nothing above. So the
 * chain is the node's own {@code parent} chain and nothing here has to know how
 * a Testin project is shaped.
 * <p>
 * Children are not offered. The bar uses them for the drop-down beside each
 * step, and a test project can hold thousands of test cases: the tree is where
 * you browse, and a bar that opened a list of two thousand would be answering a
 * question nobody asked. Every step still navigates.
 */
public class TestinNavBarItemProvider implements NavBarItemProvider {

    /**
     * UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-216.
     * <p>
     * The node above this one, and nothing above the test project - which is
     * what ends the walk, the same way {@code selfAndAncestors} ends it.
     */
    @Override
    public @Nullable NavBarItem findParent(final @NotNull NavBarItem child) {
        if (!(child instanceof TestinNavBarItem item)) return null;

        final @Nullable DirectoryDto parent = item.node().getParent();

        return parent == null ? null : new TestinNavBarItem(item.p(), parent);
    }

    @Override
    public @NotNull Iterable<NavBarItem> iterateChildren(final @NotNull NavBarItem item) {
        return List.of();
    }
}
