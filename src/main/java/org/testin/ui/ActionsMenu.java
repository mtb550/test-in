package org.testin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * What the entry that opens onto the rest is called, and what it looks like.
 * <p>
 * The tree has had one of these since its own menu grew too long, and the two
 * editors have one now. Three menus, one word and one icon, so a tester who
 * learns where things are in the tree finds them in the same place in an editor
 * - and renaming it renames all three.
 * <p>
 * <b>The word and the look, not the entries.</b> The tree's Actions holds the
 * statuses a node can be set to and what can be done to the node; an editor's
 * holds the clipboard and the history. Nothing in one belongs in the other, so
 * they are two arrangements over one shared caption rather than one menu built
 * twice.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActionsMenu {

    /**
     * An empty Actions submenu for the caller to fill.
     */
    public static @NotNull DefaultActionGroup group() {
        final @NotNull DefaultActionGroup group = new DefaultActionGroup("Actions", true);
        group.getTemplatePresentation().setIcon(AllIcons.Actions.Edit);

        return group;
    }
}
