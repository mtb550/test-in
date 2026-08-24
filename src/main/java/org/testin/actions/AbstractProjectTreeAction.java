package org.testin.actions;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * An action on the project tree: it needs the project and the tree, and twelve
 * of them declared both for themselves.
 * <p>
 * Only for actions whose tree is always there. {@code EscapeAction} and
 * {@code GenerateReportAction} have constructors that take a list or a table
 * instead, and each of those says what the action does rather than which
 * surface it was given, so neither keeps a tree at all - both extend
 * {@link AbstractProjectAction} directly.
 * <p>
 * Like its parent, this holds constructor arguments and nothing else.
 */
public abstract class AbstractProjectTreeAction extends AbstractProjectAction {

    protected final @NotNull SimpleTree tree;

    protected AbstractProjectTreeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull String title, final @NotNull String description, final @Nullable Icon icon) {
        super(p, title, description, icon);
        this.tree = tree;
    }
}
