package org.testin.navigate;

import com.intellij.model.Pointer;
import com.intellij.openapi.project.Project;
import com.intellij.platform.navbar.NavBarItemPresentation;
import com.intellij.platform.navbar.NavBarItemPresentationData;
import com.intellij.platform.navbar.backend.NavBarItem;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.search.GoTo;
import org.testin.search.Hit;

/**
 * One Testin node, as the IDE's navigation bar understands a node (#161).
 * <p>
 * The bar along the top of the window is built from PSI, and a Testin editor has
 * none - so it showed a Testin tab's bare node name and no ancestors, while a
 * Java file beside it showed its whole path. {@code NavBarItemProvider} is the
 * one extension point that speaks in items rather than {@code PsiElement}, which
 * is what lets a {@code DirectoryDto} chain be expressed at all.
 * <p>
 * <b>This is an internal platform API.</b> {@code NavBarItem} and
 * {@code NavBarItemProvider} are marked {@code @ApiStatus.Internal}, so they
 * carry no deprecation cycle and can change in any release - and Testin declares
 * {@code sinceBuild} with no upper bound, so a tester is carried onto whatever
 * IDE version arrives. The bar failing is a bar that says nothing, not a plugin
 * that will not load, and that is the whole of why the risk is acceptable here:
 * everything a tester does still works, and Testin's own status bar says where
 * they are regardless (Rule-EDITOR-PANEL-215).
 * <p>
 * Held by hard pointer because a node is a plain object served by the indexer.
 * It has no document to survive and no file to be invalidated against - the next
 * rescan replaces it, and the bar is rebuilt from the editor when that happens.
 */
public record TestinNavBarItem(@NotNull Project p, @NotNull DirectoryDto node) implements NavBarItem {

    @Override
    public @NotNull Pointer<TestinNavBarItem> createPointer() {
        return Pointer.hardPointer(this);
    }

    /**
     * UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-216.
     * <p>
     * The name and the icon the project tree already draws, asked for rather
     * than restated - the icon comes off {@code DirectoryType}, which owns one
     * per node kind, so the bar cannot come to draw a test set differently from
     * the tree.
     */
    @Override
    public @NotNull NavBarItemPresentation presentation() {
        return new NavBarItemPresentationData(node.getType().getIcon(), node.getName(), node.getName(),
                SimpleTextAttributes.REGULAR_ATTRIBUTES, true, false);
    }

    /**
     * UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-216.
     * <p>
     * A click goes where the same click goes everywhere else in Testin: the tree
     * expands to the node and its editor opens if it has one. {@code GoTo} is the
     * one owner of that, so the bar, the global search and the view panel's path
     * all mean the same thing by going to a node.
     */
    @Override
    public boolean navigateOnClick() {
        GoTo.the(p, Hit.of(node));

        return true;
    }
}
