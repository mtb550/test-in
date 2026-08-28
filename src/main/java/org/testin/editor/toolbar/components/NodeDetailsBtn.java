package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.Toolbar;
import org.testin.view.marker.MarkerDetailsViewDialog;

/**
 * Opens the Details popup for the node the editor is showing - the same popup
 * the tree's own Details action opens, on the same node.
 * <p>
 * One button for both editors. It works for either because it never asks which
 * kind it is looking at: it asks the editor for its node, and the popup reads
 * the {@link org.testin.model.markers.Marker} contract that every node's marker
 * implements. So a test set shows its status and counts and a test run shows the
 * configuration it was created with, from the same click.
 * <p>
 * The dialog is opened here rather than handed back to the editor as a callback.
 * There is nothing editor-specific to do, and a callback would have been the
 * same two lines written twice.
 * <p>
 * Not to be confused with {@link RunDetailsPopupBtn} and
 * {@link TestDetailsPopupBtn}, which despite the name choose which columns the
 * grid shows.
 */
public class NodeDetailsBtn extends AbstractIconButton implements ToolbarItem {

    public NodeDetailsBtn(final @NotNull Toolbar editor) {
        // The icon the tree's Details action already uses, so one command does
        // not look like two things depending on where it is reached from.
        super("Details", AllIcons.General.IndentDetected);

        addActionListener(e -> new MarkerDetailsViewDialog(editor.getProject(), editor.getEditedNode()).show());
    }
}
