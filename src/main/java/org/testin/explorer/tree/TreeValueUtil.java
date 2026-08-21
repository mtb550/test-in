package org.testin.explorer.tree;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * What the tree has, asked as a question. Every answer here is an
 * {@link Optional} because every one of them can honestly be nothing - the
 * panel draws a message instead of a tree, the selection is empty, it holds
 * several nodes, or it holds a node of another kind - and an action asks
 * rather than fetching a path and taking it apart itself (#71).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TreeValueUtil {

    private static final TreePath @NotNull [] NO_PATHS = new TreePath[0];
    private static final int @NotNull [] NO_ROWS = new int[0];

    /**
     * Unwraps whatever a Swing tree node carries - the async model wraps it
     * once more than the legacy paths do. Null is what a tree node with nothing
     * in it holds, and every caller reads the answer with {@code instanceof},
     * which asks both questions at once.
     */
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

    /**
     * The value a tree node carries, when it is of this kind.
     */
    public static <T> @NotNull Optional<T> valueOf(final @Nullable Object component, final @NotNull Class<T> type) {
        final Object value = valueOf(component);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    /**
     * The directory a tree node carries.
     */
    public static @NotNull Optional<DirectoryDto> directoryOf(final @Nullable Object component) {
        return valueOf(component, DirectoryDto.class);
    }

    /**
     * The directory at the end of a path.
     */
    public static @NotNull Optional<DirectoryDto> directoryAt(final @Nullable TreePath path) {
        return Optional.ofNullable(path).flatMap(at -> directoryOf(at.getLastPathComponent()));
    }

    /**
     * Whatever directory the tree has selected. Unlike {@link #singleSelected}
     * this does not care how many are selected - the creators use it to find the
     * parent to create under.
     */
    public static @NotNull Optional<DirectoryDto> selectedDirectory(final @NotNull SimpleTree tree) {
        return directoryAt(tree.getSelectionPath());
    }

    /**
     * Whatever the tree has selected, when it is of this kind.
     */
    public static <T> @NotNull Optional<T> selected(final @NotNull SimpleTree tree, final @NotNull Class<T> type) {
        return Optional.ofNullable(tree.getSelectionPath())
                .flatMap(path -> valueOf(path.getLastPathComponent(), type));
    }

    /**
     * The test project the tree is rooted at - empty in every state the panel
     * draws instead of a tree.
     */
    public static @NotNull Optional<Path> projectPath(final @NotNull SimpleTree tree) {
        return valueOf(tree.getModel().getRoot(), TestProjectDirectoryDto.class).map(DirectoryDto::getPath);
    }

    /**
     * The node selected on its own, when it is of this kind: empty when nothing
     * is selected, when more than one thing is, or when what is selected is
     * something else.
     * <p>
     * The enablement rule the tree actions share. Each of them used to spell it
     * out, so a change to what counts as a selection had to be made once per
     * action.
     */
    public static <T> @NotNull Optional<T> singleSelected(final @NotNull SimpleTree tree, final @NotNull Class<T> type) {
        return tree.getSelectionCount() == 1 ? selected(tree, type) : Optional.empty();
    }

    /**
     * The directory selected on its own.
     */
    public static @NotNull Optional<DirectoryDto> singleSelectedDirectory(final @NotNull SimpleTree tree) {
        return singleSelected(tree, DirectoryDto.class);
    }

    /**
     * Every directory among the selected paths, in selection order. Swing hands
     * over null rather than an empty array when nothing is selected, which is
     * the one place that is converted rather than asked.
     */
    public static @NotNull List<DirectoryDto> selectedDirectories(final TreePath @Nullable [] paths) {
        return Arrays.stream(Objects.requireNonNullElse(paths, NO_PATHS))
                .map(TreeValueUtil::directoryAt)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * The rows the tree has selected, and none when it has no selection - the
     * same conversion as {@link #selectedDirectories}, for the callers that
     * want screen rows rather than values.
     */
    public static int @NotNull [] selectedRows(final @NotNull SimpleTree tree) {
        return Objects.requireNonNullElse(tree.getSelectionRows(), NO_ROWS);
    }
}
