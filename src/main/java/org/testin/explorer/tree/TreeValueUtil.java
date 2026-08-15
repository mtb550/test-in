package org.testin.explorer.tree;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.dirs.DirectoryDto;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves application values from both the async model and legacy Swing paths.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TreeValueUtil {
    public static @Nullable Object valueOf(final @Nullable Object component) {
        if (component instanceof ExplorerTreeNode node) {
            return node.getValue();
        }
        if (component instanceof DefaultMutableTreeNode node) {
            return valueOf(node.getUserObject());
        }
        if (component instanceof NodeDescriptor<?> descriptor) {
            return valueOf(descriptor.getElement());
        }
        return component;
    }

    public static @Nullable DirectoryDto directoryOf(final @Nullable Object component) {
        final Object value = valueOf(component);
        return value instanceof DirectoryDto directory ? directory : null;
    }

    public static @Nullable DirectoryDto selectedDirectory(final @Nullable TreePath path) {
        return path == null ? null : directoryOf(path.getLastPathComponent());
    }

    /**
     * The directory selected on its own, or null when nothing is selected, more
     * than one thing is, or the selection is not a directory.
     * <p>
     * The enablement rule the tree actions share: "exactly one, and it is a
     * directory". Each of them used to spell it out, so a change to what counts
     * as a selection had to be made once per action.
     */
    public static @Nullable DirectoryDto singleSelectedDirectory(final @NotNull SimpleTree tree) {
        if (tree.getSelectionCount() != 1) return null;

        return selectedDirectory(tree.getSelectionPath());
    }

    public static @NotNull List<DirectoryDto> selectedDirectories(final @Nullable TreePath[] paths) {
        if (paths == null) return List.of();

        final List<DirectoryDto> values = new ArrayList<>(paths.length);
        for (final TreePath path : paths) {
            final DirectoryDto directory = selectedDirectory(path);
            if (directory != null) values.add(directory);
        }
        return values;
    }

    public static <T> @Nullable T valueOf(final @Nullable Object component, final @NotNull Class<T> type) {
        final Object value = valueOf(component);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}
