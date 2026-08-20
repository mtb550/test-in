package org.testin.explorer.tree;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.Moved;
import org.testin.codegen.SubtreeCode;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;
import org.testin.ui.framework.ConfirmDialog;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * Transfers application values and lets the async model rebuild from indexer state.
 */
public class TreeTransferHandler extends TransferHandler {
    public static final DataFlavor NODE_FLAVOR =
            new DataFlavor(TreeTransferPayload.class, "Testin tree nodes");

    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull Runnable refresh;
    @Getter
    private final @NotNull Set<DirectoryDto> selectedNodes;
    private int clipboardAction = COPY;

    public TreeTransferHandler(final @NotNull Project p, final @NotNull SimpleTree tree,
                               final @NotNull Set<DirectoryDto> selectedNodes, final @NotNull Runnable refresh) {
        this.p = p;
        this.tree = tree;
        this.selectedNodes = selectedNodes;
        this.refresh = refresh;
    }

    /**
     * Transfers never cross test projects, whatever the node types — the
     * clipboard survives switching projects, so a cut in project A must not
     * paste into project B. Unresolvable ownership rejects.
     */
    static boolean sameTestProject(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target) {
        final Optional<Path> sourceProject = owningProject(source).map(DirectoryDto::getPath);

        return sourceProject.isPresent()
                && sourceProject.equals(owningProject(target).map(DirectoryDto::getPath));
    }

    /**
     * Empty when the node hangs outside any test project - ownership
     * unresolvable.
     */
    private static @NotNull Optional<DirectoryDto> owningProject(final @NotNull DirectoryDto node) {
        DirectoryDto current = node;
        while (current != null && !(current instanceof TestProjectDirectoryDto)) {
            current = current.getParent();
        }
        return Optional.ofNullable(current);
    }

    private static @NotNull String describe(final @NotNull List<DirectoryDto> sources) {
        return sources.size() == 1 ? "'" + sources.getFirst().getName() + "'" : sources.size() + " items";
    }

    /**
     * The destination must not be the node itself, inside its own subtree, or
     * its current parent — and must not already contain a node with the same
     * name. Any of those makes the VFS operation fail with an IO error
     * ("already exists"). Applies to copy and move alike. The occupied check
     * is injected so the rules stay testable without an indexer.
     */
    static boolean isValidDestination(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target,
                                      final @NotNull Predicate<Path> occupied) {
        final Path sourcePath = source.getPath().normalize();
        final Path targetPath = target.getPath().normalize();

        if (sourcePath.equals(targetPath) || targetPath.startsWith(sourcePath)) return false;
        if (targetPath.equals(sourcePath.getParent())) return false;
        return !occupied.test(targetPath.resolve(sourcePath.getFileName()));
    }

    @Override
    public int getSourceActions(final @NotNull JComponent c) {
        return COPY_OR_MOVE;
    }

    /**
     * Swing's contract: null is how a TransferHandler says there is nothing to
     * drag, and the platform reads it before anything of ours does (#71).
     */
    @Override
    protected @Nullable Transferable createTransferable(final @NotNull JComponent c) {
        final List<DirectoryDto> directories = transferableSelection();
        if (directories.isEmpty()) return null;

        // A styled ghost instead of Swing's raw black box.
        setDragImage(createDragImage(describe(directories)));
        setDragImageOffset(new Point(JBUI.scale(-14), JBUI.scale(-10)));

        return new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), clipboardAction));
    }

    /**
     * A small theme-colored pill with the dragged name or count.
     */
    private @NotNull BufferedImage createDragImage(final @NotNull String text) {
        final Font font = tree.getFont();
        final FontMetrics metrics = tree.getFontMetrics(font);
        final int padX = JBUI.scale(10);
        final int padY = JBUI.scale(5);
        final int width = metrics.stringWidth(text) + padX * 2;
        final int height = metrics.getHeight() + padY * 2;
        final int arc = JBUI.scale(10);

        final BufferedImage image = ImageUtil.createImage(tree.getGraphicsConfiguration(), width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
            g.setColor(UIUtil.getListSelectionBackground(true));
            g.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);
            g.setColor(UIUtil.getListSelectionForeground(true));
            g.setFont(font);
            g.drawString(text, padX, padY + metrics.getAscent());
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Whether copying or cutting would put anything on the clipboard - which is
     * true of the nodes that declare themselves transferable, and no others.
     * <p>
     * The menu entries ask this rather than deciding for themselves, so what a
     * greyed Copy means and what Copy would do are the same rule read twice.
     */
    public boolean hasTransferableSelection() {
        return !transferableSelection().isEmpty();
    }

    /**
     * Whether what is on the clipboard could land on what is selected.
     * <p>
     * The same question the paste itself asks, through the same method - the
     * flavor, the target, the family rules and the own-subtree check all
     * together. A menu entry deciding any part of that for itself would be a
     * second rule that agrees with this one until the day it does not.
     */
    public boolean canPasteFromClipboard() {
        final Transferable contents = CopyPasteManager.getInstance().getContents();
        return contents != null && canImport(new TransferSupport(tree, contents));
    }

    private @NotNull List<DirectoryDto> transferableSelection() {
        return TreeValueUtil.selectedDirectories(tree.getSelectionPaths()).stream()
                .filter(DirectoryDto::isTransferable)
                .toList();
    }

    @Override
    public boolean canImport(final @NotNull TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;
        final boolean valid = targetDirectory(support)
                .filter(DirectoryDto::isTransferTarget)
                .filter(target -> anySourceLands(support, target))
                .isPresent();

        // No drop highlight over places nothing can land on - the highlight
        // otherwise lingers as a stray selection band.
        if (support.isDrop()) support.setShowDropLocation(valid);
        if (!valid) return false;

        if (support.isDrop() && support.getDropAction() == NONE) {
            support.setDropAction(MOVE);
        }
        return true;
    }

    /**
     * Live drag feedback: the no-drop cursor appears over targets that would
     * reject everything being dragged (family rules, own parent, own subtree).
     * Same-JVM transfer data is readable during dragOver; if the platform
     * refuses, stay permissive - importData re-checks everything anyway.
     */
    private boolean anySourceLands(final @NotNull TransferSupport support, final @NotNull DirectoryDto target) {
        try {
            final TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            for (final DirectoryDto source : payload.nodes()) {
                if (canTransferInto(source, target)) return true;
            }
            return false;
        } catch (final Exception ex) {
            return true;
        }
    }

    @Override
    public boolean importData(final @NotNull TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            final Optional<DirectoryDto> landing = targetDirectory(support);
            if (landing.isEmpty()) return false;
            final DirectoryDto target = landing.get();

            final int action = resolveAction(support, payload);
            final List<DirectoryDto> sources = transferableSources(payload.nodes(), target, action);

            // Clipboard pastes are notified by PasteNodeAction before this runs.
            if (support.isDrop()) notifyNameCollisions(payload.nodes(), target);
            if (sources.isEmpty()) return false;

            // Drag-drop confirms before changing anything; clipboard paste
            // already confirmed in PasteNodeAction.
            if (support.isDrop()) {
                final String verb = action == COPY ? "Copy" : "Move";
                final Path fromPath = sources.getFirst().getPath().getParent();
                new ConfirmDialog(p, verb,
                        verb + " " + describe(sources) + " into '" + target.getName() + "'?",
                        fromPath == null ? "" : fromPath.toString(),
                        target.getPath().toString(),
                        verb,
                        () -> transfer(action, sources, target)
                ).show();
                return true;
            }

            transfer(action, sources, target);
            return true;
        } catch (final Exception ex) {
            Logger.error("Tree transfer failed: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Only the sources the target accepts and that can actually land on it.
     */
    private @NotNull List<DirectoryDto> transferableSources(final DirectoryDto @NotNull [] nodes,
                                                            final @NotNull DirectoryDto target, final int action) {
        if (action != COPY && action != MOVE) return List.of();

        final List<DirectoryDto> accepted = new ArrayList<>();
        for (final DirectoryDto source : nodes) {
            if (canTransferInto(source, target)) accepted.add(source);
        }
        return accepted;
    }

    /**
     * True when the source may be pasted or dropped into the target at all.
     */
    public boolean canTransferInto(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target) {
        // Existence comes from the indexer cache - file access is the
        // indexer's alone (see CLAUDE.md).
        return target.acceptsTransferred(source)
                && sameTestProject(source, target)
                && isValidDestination(source, target, path -> Services.getInstance(p, ProjectIndexer.class).nodeExists(path));
    }

    /**
     * True when only a name collision at the target blocks this source.
     */
    private boolean isNameCollision(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target) {
        return target.acceptsTransferred(source)
                && isValidDestination(source, target, path -> false)
                && Services.getInstance(p, ProjectIndexer.class).nodeExists(target.getPath().resolve(source.getName()));
    }

    /**
     * Small soft balloon naming what could not land because the name is taken.
     */
    public void notifyNameCollisions(final DirectoryDto @NotNull [] nodes, final @NotNull DirectoryDto target) {
        final List<DirectoryDto> collided = new ArrayList<>();
        for (final DirectoryDto source : nodes) {
            if (isNameCollision(source, target)) collided.add(source);
        }
        if (collided.isEmpty()) return;

        final String verb = collided.size() == 1 ? " already exists in '" : " already exist in '";
        Services.getInstance(p, Notifier.class).softShow(p, describe(collided) + verb + target.getName() + "'");
    }

    private void transfer(final int action, final @NotNull List<DirectoryDto> sources,
                          final @NotNull DirectoryDto target) {
        if (action == MOVE) {
            moveNodes(sources, target);
        } else {
            final List<Path> sourcePaths = sources.stream().map(DirectoryDto::getPath).toList();
            Services.getInstance(p, ProjectIndexer.class).copyNodes(sourcePaths, target.getPath(), copied -> {
                generateForCopies(sources, target);
                refresh.run();
                confirmLanded("Pasted", copied);
            });
        }

        resetLastAction();
    }

    /**
     * Confirms what actually arrived, for both the clipboard paste and the drop.
     * The count comes from the indexer, which reports how many of the operations
     * it ran succeeded (#66, F2).
     */
    private void confirmLanded(final @NotNull String outcome, final int landed) {
        if (landed == 0) return;

        Services.getInstance(p, Notifier.class).softShowCounted(p, outcome, landed);
    }

    /**
     * Where the transfer would land: the row under a drop, or whatever the tree
     * has selected for a clipboard paste.
     */
    private @NotNull Optional<DirectoryDto> targetDirectory(final @NotNull TransferSupport support) {
        return support.isDrop()
                ? dropPath(support).flatMap(TreeValueUtil::directoryAt)
                : TreeValueUtil.selectedDirectory(tree);
    }

    /**
     * SimpleTree has a drop location type of its own, and reporting the drop by
     * the wrong one is how ordering silently refused every drop it was given.
     */
    private @NotNull Optional<TreePath> dropPath(final @NotNull TransferSupport support) {
        if (support.getDropLocation() instanceof SimpleTree.DropLocation dropLocation) {
            return Optional.ofNullable(dropLocation.getPath());
        }
        if (support.getDropLocation() instanceof JTree.DropLocation dropLocation) {
            return Optional.ofNullable(dropLocation.getPath());
        }
        return Optional.empty();
    }

    private int resolveAction(final @NotNull TransferSupport support, final @NotNull TreeTransferPayload payload) {
        if (!support.isDrop()) {
            return payload.clipboardAction() == MOVE ? MOVE : COPY;
        }

        // Keep Ctrl-drag copy support. A normal internal tree drag is a move,
        // including platforms that report NONE before the drop action is set.
        if (support.getUserDropAction() == COPY) return COPY;
        support.setDropAction(MOVE);
        return MOVE;
    }

    private void moveNodes(final @NotNull List<DirectoryDto> sources, final @NotNull DirectoryDto target) {
        // Captured before the move - the DTO paths change underneath.
        final List<Path> oldPaths = sources.stream().map(DirectoryDto::getPath).toList();
        final List<Path> newPaths = sources.stream()
                .map(source -> target.getPath().resolve(source.getName()))
                .toList();

        moveBatch(oldPaths, newPaths, moved -> confirmLanded("Moved", moved));

        Services.getInstance(p, TreeUndoService.class).push(new TreeUndoService.TreeOperation(
                "Move " + describe(sources),
                () -> moveBatch(newPaths, oldPaths),
                () -> moveBatch(oldPaths, newPaths)));
    }

    private void moveBatch(final @NotNull List<Path> from, final @NotNull List<Path> to) {
        moveBatch(from, to, moved -> {
        });
    }

    /**
     * The undo and redo reverses pass no {@code onDone}: they are confirmed as
     * "Undone" and "Redone" by their own actions, and a second balloon saying
     * the nodes moved would double-report one keystroke.
     */
    private void moveBatch(final @NotNull List<Path> from, final @NotNull List<Path> to,
                           final @NotNull IntConsumer onDone) {
        final AtomicInteger remaining = new AtomicInteger(from.size());
        final AtomicInteger moved = new AtomicInteger();

        for (int i = 0; i < from.size(); i++) {
            // Before the data move, while the old path is still what finds the
            // generated code - and here rather than at the gesture, because undo
            // and redo are this same routine with the two lists swapped, so they
            // carry the code back and forth without knowing they do (#51).
            syncCode(from.get(i), to.get(i));

            Services.getInstance(p, ProjectIndexer.class).moveNode(from.get(i), to.get(i), wasMoved -> {
                // The move is asynchronous, so this can land after the project
                // closed; refreshing a disposed tree throws.
                if (p.isDisposed()) return;
                if (wasMoved) moved.incrementAndGet();
                if (remaining.decrementAndGet() != 0) return;

                refresh.run();
                onDone.accept(moved.get());
            });
        }
    }

    /**
     * Generates the Java for what was just copied.
     * <p>
     * A copy has none of its own: the files were duplicated, and nothing in them
     * is Java. Unlike a move there is nothing to carry over, so each copied node
     * and everything under it is generated from scratch - which is also why this
     * runs after the copy rather than before it, the opposite of a move (#51).
     */
    private void generateForCopies(final @NotNull List<DirectoryDto> sources, final @NotNull DirectoryDto target) {
        if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return;

        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        for (final DirectoryDto source : sources) {
            indexer.find(target.getPath().resolve(source.getName())).ifPresent(copy -> SubtreeCode.generate(p, copy));
        }
    }

    /**
     * Moves the generated Java that belongs to the node at this path, if the
     * node has any. Which generator that is belongs to the node itself.
     */
    private void syncCode(final @NotNull Path from, final @NotNull Path to) {
        if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return;

        final Path target = to.getParent();
        if (target == null) return;

        Services.getInstance(p, ProjectIndexer.class).find(from)
                .ifPresent(dir -> dir.getType().getMoveCodegen().execute(p, new Moved(dir, target)));
    }

    // Both parameters are Swing's, and Swing passes null for either when the
    // drag ended without one (#71).
    @Override
    protected void exportDone(final @Nullable JComponent source, final @Nullable Transferable data, final int action) {
        if (action != MOVE) resetLastAction();
    }

    public void resetLastAction() {
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(final @NotNull JComponent comp, final @NotNull Clipboard clip, final int action) {
        clipboardAction = action;
        try {
            super.exportToClipboard(comp, clip, action);
        } finally {
            clipboardAction = COPY;
        }
        updateClipboardState(action, transferableSelection());
    }

    public void copySelectionToClipboard(final boolean cut) {
        final List<DirectoryDto> directories = transferableSelection();
        if (directories.isEmpty()) return;

        final int action = cut ? MOVE : COPY;
        CopyPasteManager.getInstance().setContents(new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), action)));
        updateClipboardState(action, directories);

        // Here rather than in CopyNodeAction and CutNodeAction: this is the point
        // that knows the clipboard was actually written, how many nodes went on
        // it, and which of the two it was. A copy changes nothing on screen, so
        // without this the tester has no way to tell it happened.
        Services.getInstance(p, Notifier.class)
                .softShowCounted(p, cut ? "Cut" : "Copied", directories.size());
    }

    public void pasteFromClipboard() {
        final Transferable contents = CopyPasteManager.getInstance().getContents();
        if (contents != null) importData(new TransferSupport(tree, contents));
    }

    private void updateClipboardState(final int action, final @NotNull List<DirectoryDto> directories) {
        selectedNodes.clear();
        if (action == MOVE) selectedNodes.addAll(directories);
        tree.repaint();
    }
}
