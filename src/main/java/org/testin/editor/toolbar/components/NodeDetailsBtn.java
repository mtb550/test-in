package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the Details popup for the node the editor is showing - the same popup
 * the tree's own Details action opens, on the same node.
 * <p>
 * Named for the node rather than the run: what it opens is a node's details, and
 * a test set has one too. It is on the run toolbar because that is where it was
 * asked for.
 * <p>
 * Not to be confused with {@link RunDetailsPopupBtn}, which despite the name
 * chooses which columns the grid shows.
 */
public class NodeDetailsBtn extends AbstractButton implements ToolbarItem {

    public NodeDetailsBtn(final @NotNull Runnable onToolBarNodeDetailsClicked) {
        // The icon the tree's Details action already uses, so one command does
        // not look like two things depending on where it is reached from.
        super("Details", AllIcons.General.IndentDetected);

        addActionListener(e -> onToolBarNodeDetailsClicked.run());
    }
}
