/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.explorer.tree;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaCode;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.Moved;
import org.testin.codegen.SubtreeCode;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoHistories;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;
import org.testin.actions.TestinData;
import org.testin.ui.framework.ConfirmDialog;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

public class TreeTransferHandler extends TransferHandler {
    public static final @NotNull DataFlavor NODE_FLAVOR =
            new DataFlavor(TreeTransferPayload.class, "Testin tree nodes");

    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull Runnable refresh;

    private final @NotNull Consumer<Path> refreshAndReveal;
    @Getter
    private final @NotNull Set<DirectoryDto> selectedNodes;
    private int clipboardAction = COPY;

    public TreeTransferHandler(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull Set<DirectoryDto> selectedNodes, final @NotNull Runnable refresh, final @NotNull Consumer<Path> refreshAndReveal) {
        this.p = p;
        this.tree = tree;
        this.selectedNodes = selectedNodes;
        this.refresh = refresh;
        this.refreshAndReveal = refreshAndReveal;
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    public static @NotNull Optional<TreeTransferHandler> of(final @NotNull AnActionEvent e) {
        return TestinData.tree(e)
                .map(JComponent::getTransferHandler)
                .filter(TreeTransferHandler.class::isInstance)
                .map(TreeTransferHandler.class::cast);
    }

    // Rule-TREE-PANEL-013
    static boolean sameTestProject(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target) {
        final @NotNull Optional<Path> sourceProject = owningProject(source).map(DirectoryDto::getPath);

        return sourceProject.isPresent()
                && sourceProject.equals(owningProject(target).map(DirectoryDto::getPath));
    }

    private static @NotNull Optional<DirectoryDto> owningProject(final @NotNull DirectoryDto node) {
        return node.selfAndAncestors().stream()
                .filter(TestProjectDirectoryDto.class::isInstance)
                .findFirst();
    }

    private static @NotNull String describe(final @NotNull List<DirectoryDto> sources) {
        return sources.size() == 1
                ? "'" + sources.getFirst().getName() + "'"
                : Bundle.message("transfer.items", String.valueOf(sources.size()));
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-004, Rule-TREE-PANEL-045
    static boolean isValidDestination(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target, final @NotNull Predicate<Path> occupied) {
        final @NotNull Path sourcePath = source.getPath().normalize();
        final @NotNull Path targetPath = target.getPath().normalize();

        if (sourcePath.equals(targetPath) || targetPath.startsWith(sourcePath)) return false;
        if (targetPath.equals(sourcePath.getParent())) return false;
        return !occupied.test(targetPath.resolve(sourcePath.getFileName()));
    }

    @Override
    public int getSourceActions(final @NotNull JComponent c) {
        return COPY_OR_MOVE;
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    @Override
    protected @Nullable Transferable createTransferable(final @NotNull JComponent c) {
        final @NotNull List<DirectoryDto> directories = transferableSelection();
        if (directories.isEmpty()) return null;

        setDragImage(createDragImage(describe(directories)));
        setDragImageOffset(new Point(JBUI.scale(-14), JBUI.scale(-10)));

        return new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), clipboardAction));
    }

    private @NotNull BufferedImage createDragImage(final @NotNull String text) {
        final @NotNull Font font = tree.getFont();
        final @NotNull FontMetrics metrics = tree.getFontMetrics(font);
        final int padX = JBUI.scale(10);
        final int padY = JBUI.scale(5);
        final int width = metrics.stringWidth(text) + padX * 2;
        final int height = metrics.getHeight() + padY * 2;
        final int arc = JBUI.scale(10);

        final @NotNull BufferedImage image = ImageUtil.createImage(tree.getGraphicsConfiguration(), width, height, BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();
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

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-002
    public boolean hasTransferableSelection() {
        return !transferableSelection().isEmpty();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-002, Rule-TREE-PANEL-044
    public boolean canPasteFromClipboard() {
        final @NotNull Optional<DirectoryDto> target = TreeValues.singleSelectedDirectory(tree).filter(DirectoryDto::isTransferTarget);
        if (target.isEmpty()) return false;

        return clipboardNodes().stream().anyMatch(node -> canTransferInto(node, target.orElseThrow()));
    }

    private @NotNull List<DirectoryDto> clipboardNodes() {
        return ClipboardContents.withFlavor(NODE_FLAVOR)
                .map(this::nodesOf)
                .orElseGet(List::of);
    }

    private @NotNull List<DirectoryDto> nodesOf(final @NotNull Transferable contents) {
        try {
            return List.of(((TreeTransferPayload) contents.getTransferData(NODE_FLAVOR)).nodes());
        } catch (final Exception ex) {
            Logger.debug("Clipboard no longer holds tree nodes: " + ex.getMessage());
            return List.of();
        }
    }

    private @NotNull List<DirectoryDto> transferableSelection() {
        return TreeValues.selectedDirectories(tree.getSelectionPaths()).stream()
                .filter(DirectoryDto::isTransferable)
                .toList();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-046
    @Override
    public boolean canImport(final @NotNull TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;
        final boolean valid = targetDirectory(support)
                .filter(DirectoryDto::isTransferTarget)
                .filter(target -> anySourceLands(support, target))
                .isPresent();

        if (support.isDrop()) support.setShowDropLocation(valid);
        if (!valid) return false;

        if (support.isDrop() && support.getDropAction() == NONE) {
            support.setDropAction(MOVE);
        }
        return true;
    }

    private boolean anySourceLands(final @NotNull TransferSupport support, final @NotNull DirectoryDto target) {
        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            for (final DirectoryDto source : payload.nodes()) {
                if (canTransferInto(source, target)) return true;
            }
            return false;
        } catch (final Exception ex) {
            return true;
        }
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-006
    @Override
    public boolean importData(final @NotNull TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            final @NotNull Optional<DirectoryDto> landing = targetDirectory(support);
            if (landing.isEmpty()) return false;
            final @NotNull DirectoryDto target = landing.get();

            final int action = resolveAction(support, payload);
            final @NotNull List<DirectoryDto> sources = transferableSources(payload.nodes(), target);

            if (support.isDrop()) notifyNameCollisions(payload.nodes(), target);
            if (sources.isEmpty()) return false;

            if (support.isDrop()) {
                final @NotNull String verb = action == COPY ? Bundle.message("transfer.copy") : Bundle.message("transfer.move");
                final @NotNull Path fromPath = sources.getFirst().getPath().getParent();
                new ConfirmDialog(p, verb,
                        Bundle.message("transfer.confirm", verb, describe(sources), target.getName()),
                        Objects.toString(fromPath, ""),
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

    private @NotNull List<DirectoryDto> transferableSources(final DirectoryDto @NotNull [] nodes, final @NotNull DirectoryDto target) {
        final @NotNull List<DirectoryDto> accepted = new ArrayList<>();
        for (final DirectoryDto source : nodes) {
            if (canTransferInto(source, target)) accepted.add(source);
        }
        return accepted;
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-043, Rule-TREE-PANEL-044, Rule-TREE-PANEL-045
    public boolean canTransferInto(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target) {
        return target.acceptsTransferred(source)
                && sameTestProject(source, target)
                && isValidDestination(source, target, path -> Services.getInstance(p, ProjectIndexer.class).nodeExists(path));
    }

    private boolean isNameCollision(final @NotNull DirectoryDto source, final @NotNull DirectoryDto target) {
        return target.acceptsTransferred(source)
                && isValidDestination(source, target, path -> false)
                && Services.getInstance(p, ProjectIndexer.class).nodeExists(target.getPath().resolve(source.getName()));
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-004
    public boolean notifyNameCollisions(final DirectoryDto @NotNull [] nodes, final @NotNull DirectoryDto target) {
        final @NotNull List<DirectoryDto> collided = new ArrayList<>();
        for (final DirectoryDto source : nodes) {
            if (isNameCollision(source, target)) collided.add(source);
        }
        if (collided.isEmpty()) return false;

        final @NotNull String said = collided.size() == 1
                ? Bundle.message("transfer.exists.one", describe(collided), target.getName())
                : Bundle.message("transfer.exists.many", describe(collided), target.getName());
        Services.getInstance(p, Notifier.class).softRefuse(p, said);

        return true;
    }

    private void transfer(final int action, final @NotNull List<DirectoryDto> sources, final @NotNull DirectoryDto target) {
        if (action == MOVE) {
            moveNodes(sources, target);
        } else {
            final @NotNull List<Path> sourcePaths = sources.stream().map(DirectoryDto::getPath).toList();
            Services.getInstance(p, ProjectIndexer.class).copyNodes(sourcePaths, target.getPath(), copied -> {
                generateForCopies(sources, target);

                refreshAndReveal.accept(target.getPath().resolve(sources.getFirst().getName()));

                confirmLanded(Done.PASTED, copied);
            });
        }
    }

    private void confirmLanded(final @NotNull Done outcome, final int landed) {
        if (landed == 0) return;

        Services.getInstance(p, Notifier.class).softShowCounted(p, outcome, landed);
    }

    private @NotNull Optional<DirectoryDto> targetDirectory(final @NotNull TransferSupport support) {
        return support.isDrop()
                ? dropPath(support).flatMap(TreeValues::directoryAt)
                : TreeValues.selectedDirectory(tree);
    }

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

        if (support.getUserDropAction() == COPY) return COPY;
        support.setDropAction(MOVE);
        return MOVE;
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-047
    private void moveNodes(final @NotNull List<DirectoryDto> sources, final @NotNull DirectoryDto target) {
        final @NotNull List<Path> oldPaths = sources.stream().map(DirectoryDto::getPath).toList();
        final @NotNull List<Path> newPaths = sources.stream()
                .map(source -> target.getPath().resolve(source.getName()))
                .toList();

        moveBatch(oldPaths, newPaths, moved -> {
            confirmLanded(Done.MOVED, moved);
            if (moved == 0) return;

            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                    Bundle.message("transfer.undo.move", describe(sources)),
                    () -> moveBatch(newPaths, oldPaths),
                    () -> moveBatch(oldPaths, newPaths)));
        });
    }

    private void moveBatch(final @NotNull List<Path> from, final @NotNull List<Path> to) {
        moveBatch(from, to, moved -> {
        });
    }

    private void moveBatch(final @NotNull List<Path> from, final @NotNull List<Path> to, final @NotNull IntConsumer onDone) {
        final @NotNull AtomicInteger remaining = new AtomicInteger(from.size());
        final @NotNull AtomicInteger moved = new AtomicInteger();

        syncCode(from, to);

        for (int i = 0; i < from.size(); i++) {
            final @NotNull Path source = from.get(i);

            Services.getInstance(p, ProjectIndexer.class).moveNode(source, to.get(i), wasMoved -> {
                if (p.isDisposed()) return;

                if (wasMoved) moved.incrementAndGet();
                else putCodeBack(source);

                if (remaining.decrementAndGet() != 0) return;

                refresh.run();
                onDone.accept(moved.get());
            });
        }
    }

    // Rule-CODEGEN-082
    private void generateForCopies(final @NotNull List<DirectoryDto> sources, final @NotNull DirectoryDto target) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        for (final DirectoryDto source : sources) {
            indexer.find(target.getPath().resolve(source.getName())).ifPresent(copy -> SubtreeCode.generate(p, copy));
        }
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-048, Rule-CODEGEN-082
    private void syncCode(final @NotNull List<Path> from, final @NotNull List<Path> to) {
        WriteCommandAction.runWriteCommandAction(p, Bundle.message("transfer.move.code.command"), null, () -> {
            for (int i = 0; i < from.size(); i++) moveCodeOf(from.get(i), to.get(i));
        });
    }

    // UC-TREE-PANEL-016, Rule-TREE-PANEL-098
    private void putCodeBack(final @NotNull Path source) {
        Logger.warn("Move refused for " + source.getFileName() + "; putting its generated code back.");

        syncCode(List.of(source), List.of(source));
    }

    private void moveCodeOf(final @NotNull Path from, final @NotNull Path to) {
        Optional.ofNullable(to.getParent()).ifPresent(target ->
                Services.getInstance(p, ProjectIndexer.class).find(from)
                        .ifPresent(dir -> JavaCode.of(dir.getType()).getMoved().execute(p, new Moved(dir, target))));
    }

    private void resetLastAction() {
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

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-007
    public void copySelectionToClipboard(final boolean cut) {
        final @NotNull List<DirectoryDto> directories = transferableSelection();
        if (directories.isEmpty()) return;

        final int action = cut ? MOVE : COPY;
        CopyPasteManager.getInstance().setContents(new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), action)));
        updateClipboardState(action, directories);

        Services.getInstance(p, Notifier.class)
                .softShowCounted(p, cut ? Done.CUT : Done.COPIED, directories.size());
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050, Rule-TREE-PANEL-006
    public void pasteFromClipboard(final @NotNull DirectoryDto target) {
        ClipboardContents.withFlavor(NODE_FLAVOR).ifPresent(contents -> {
            final boolean wasCut = isCut(contents);

            final @NotNull List<DirectoryDto> sources = nodesOf(contents).stream()
                    .filter(node -> canTransferInto(node, target))
                    .toList();
            if (sources.isEmpty()) return;

            transfer(wasCut ? MOVE : COPY, sources, target);
            if (wasCut) clearClipboard();
        });
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    private boolean isCut(final @NotNull Transferable contents) {
        try {
            return ((TreeTransferPayload) contents.getTransferData(NODE_FLAVOR)).clipboardAction() == MOVE;
        } catch (final Exception ex) {
            Logger.debug("Clipboard no longer holds tree nodes: " + ex.getMessage());
            return false;
        }
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    public void clearClipboard() {
        if (ClipboardContents.withFlavor(NODE_FLAVOR).isPresent()) {
            CopyPasteManager.getInstance().setContents(new StringSelection(""));
        }

        resetLastAction();
    }

    private void updateClipboardState(final int action, final @NotNull List<DirectoryDto> directories) {
        selectedNodes.clear();
        if (action == MOVE) selectedNodes.addAll(directories);
        tree.repaint();
    }
}
