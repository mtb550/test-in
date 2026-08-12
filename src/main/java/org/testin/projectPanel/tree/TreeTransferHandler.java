package org.testin.projectPanel.tree;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Transfers application values and lets the async model rebuild from indexer state.
 */
public class TreeTransferHandler extends TransferHandler {
    public static final DataFlavor NODE_FLAVOR =
            new DataFlavor(TreeTransferPayload.class, "Testin tree nodes");

    private final @NotNull Project p;
    private final SimpleTree tree;
    private final Runnable refresh;
    @Getter
    private final Set<DirectoryDto> selectedNodes;
    private int clipboardAction = COPY;

    public TreeTransferHandler(final @NotNull Project p, final SimpleTree tree,
                               final Set<DirectoryDto> selectedNodes, final Runnable refresh) {
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
    static boolean sameTestProject(final DirectoryDto source, final DirectoryDto target) {
        final DirectoryDto sourceProject = owningProject(source);
        final DirectoryDto targetProject = owningProject(target);

        return sourceProject != null && targetProject != null
                && sourceProject.getPath().equals(targetProject.getPath());
    }

    private static DirectoryDto owningProject(final DirectoryDto node) {
        DirectoryDto current = node;
        while (current != null && !(current instanceof TestProjectDirectoryDto)) {
            current = current.getParent();
        }
        return current;
    }

    private static String describe(final List<DirectoryDto> sources) {
        return sources.size() == 1 ? "'" + sources.getFirst().getName() + "'" : sources.size() + " items";
    }

    /**
     * The destination must not be the node itself, inside its own subtree, or
     * its current parent — and must not already contain a node with the same
     * name. Any of those makes the VFS operation fail with an IO error
     * ("already exists"). Applies to copy and move alike. The occupied check
     * is injected so the rules stay testable without an indexer.
     */
    static boolean isValidDestination(final DirectoryDto source, final DirectoryDto target, final Predicate<Path> occupied) {
        final Path sourcePath = source.getPath().normalize();
        final Path targetPath = target.getPath().normalize();

        if (sourcePath.equals(targetPath) || targetPath.startsWith(sourcePath)) return false;
        if (targetPath.equals(sourcePath.getParent())) return false;
        return !occupied.test(targetPath.resolve(sourcePath.getFileName()));
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
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
    private BufferedImage createDragImage(final String text) {
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
     * Only nodes that declare themselves transferable can be cut, copied or dragged.
     */
    private List<DirectoryDto> transferableSelection() {
        return TreeValueUtil.selectedDirectories(tree.getSelectionPaths()).stream()
                .filter(DirectoryDto::isTransferable)
                .toList();
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;
        final DirectoryDto target = targetDirectory(support);
        final boolean valid = target != null && target.isTransferTarget() && anySourceLands(support, target);

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
    private boolean anySourceLands(final TransferSupport support, final DirectoryDto target) {
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
    public boolean importData(final TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            final DirectoryDto target = targetDirectory(support);
            if (target == null) return false;

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
                        fromPath == null ? null : fromPath.toString(),
                        target.getPath().toString(),
                        verb,
                        () -> transfer(action, sources, target)
                ).show();
                return true;
            }

            return transfer(action, sources, target);
        } catch (final Exception ex) {
            Logger.error("Tree transfer failed: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Only the sources the target accepts and that can actually land on it.
     */
    private List<DirectoryDto> transferableSources(final DirectoryDto[] nodes, final DirectoryDto target, final int action) {
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
    public boolean canTransferInto(final DirectoryDto source, final DirectoryDto target) {
        // Existence comes from the indexer cache - file access is the
        // indexer's alone (see CLAUDE.md).
        return target.acceptsTransferred(source)
                && sameTestProject(source, target)
                && isValidDestination(source, target, path -> Services.getInstance(p, ProjectIndexer.class).nodeExists(path));
    }

    /**
     * True when only a name collision at the target blocks this source.
     */
    private boolean isNameCollision(final DirectoryDto source, final DirectoryDto target) {
        return target.acceptsTransferred(source)
                && isValidDestination(source, target, path -> false)
                && Services.getInstance(p, ProjectIndexer.class).nodeExists(target.getPath().resolve(source.getName()));
    }

    /**
     * Small soft balloon naming what could not land because the name is taken.
     */
    public void notifyNameCollisions(final DirectoryDto[] nodes, final DirectoryDto target) {
        final List<DirectoryDto> collided = new ArrayList<>();
        for (final DirectoryDto source : nodes) {
            if (isNameCollision(source, target)) collided.add(source);
        }
        if (collided.isEmpty()) return;

        final String verb = collided.size() == 1 ? " already exists in '" : " already exist in '";
        Services.getInstance(p, Notifier.class).softShow(p, describe(collided) + verb + target.getName() + "'");
    }

    private boolean transfer(final int action, final List<DirectoryDto> sources, final DirectoryDto target) {
        if (action == MOVE) {
            moveNodes(sources, target);
        } else {
            final List<Path> sourcePaths = sources.stream().map(DirectoryDto::getPath).toList();
            Services.getInstance(p, ProjectIndexer.class).copyNodes(sourcePaths, target.getPath(), refresh);
        }

        resetLastAction();
        return true;
    }

    private DirectoryDto targetDirectory(final TransferSupport support) {
        if (support.isDrop()) {
            final TreePath path = dropPath(support);
            return path == null ? null : TreeValueUtil.directoryOf(path.getLastPathComponent());
        }
        return TreeValueUtil.selectedDirectory(tree.getSelectionPath());
    }

    private TreePath dropPath(final TransferSupport support) {
        if (support.getDropLocation() instanceof SimpleTree.DropLocation dropLocation) {
            return dropLocation.getPath();
        }
        if (support.getDropLocation() instanceof JTree.DropLocation dropLocation) {
            return dropLocation.getPath();
        }
        return null;
    }

    private int resolveAction(final TransferSupport support, final TreeTransferPayload payload) {
        if (!support.isDrop()) {
            return payload.clipboardAction() == MOVE ? MOVE : COPY;
        }

        // Keep Ctrl-drag copy support. A normal internal tree drag is a move,
        // including platforms that report NONE before the drop action is set.
        if (support.getUserDropAction() == COPY) return COPY;
        support.setDropAction(MOVE);
        return MOVE;
    }

    private void moveNodes(final List<DirectoryDto> sources, final DirectoryDto target) {
        // Captured before the move - the dtos' paths change underneath.
        final List<Path> oldPaths = sources.stream().map(DirectoryDto::getPath).toList();
        final List<Path> newPaths = sources.stream()
                .map(source -> target.getPath().resolve(source.getName()))
                .toList();

        moveBatch(oldPaths, newPaths);

        Services.getInstance(p, TreeUndoService.class).push(new TreeUndoService.TreeOperation(
                "Move " + describe(sources),
                () -> moveBatch(newPaths, oldPaths),
                () -> moveBatch(oldPaths, newPaths)));
    }

    private void moveBatch(final List<Path> from, final List<Path> to) {
        final AtomicInteger remaining = new AtomicInteger(from.size());
        for (int i = 0; i < from.size(); i++) {
            Services.getInstance(p, ProjectIndexer.class).moveNode(from.get(i), to.get(i), () -> {
                if (remaining.decrementAndGet() == 0) refresh.run();
            });
        }
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        if (action != MOVE) resetLastAction();
    }

    public void resetLastAction() {
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(final JComponent comp, final Clipboard clip, final int action) {
        clipboardAction = action;
        try {
            super.exportToClipboard(comp, clip, action);
        } finally {
            clipboardAction = COPY;
        }
        updateClipboardState(action, transferableSelection());
    }

    public boolean copySelectionToClipboard(final boolean cut) {
        final List<DirectoryDto> directories = transferableSelection();
        if (directories.isEmpty()) return false;

        final int action = cut ? MOVE : COPY;
        CopyPasteManager.getInstance().setContents(new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), action)));
        updateClipboardState(action, directories);
        return true;
    }

    public boolean pasteFromClipboard() {
        final Transferable contents = CopyPasteManager.getInstance().getContents();
        return contents != null && importData(new TransferSupport(tree, contents));
    }

    private void updateClipboardState(final int action, final List<DirectoryDto> directories) {
        selectedNodes.clear();
        if (action == MOVE) selectedNodes.addAll(directories);
        tree.repaint();
    }

}
