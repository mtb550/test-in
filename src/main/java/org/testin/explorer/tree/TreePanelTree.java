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

import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.explorer.TreePanel;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class TreePanelTree implements Disposable {
    private static final @NotNull Runnable NOTHING_AFTER = () -> {
    };

    private final @NotNull Project p;
    private final @NotNull TreePanel tp;
    private final @NotNull JBScrollPane scrollPane;
    private final @NotNull TreePanelStructure treeStructure;
    private final @NotNull StructureTreeModel<TreePanelStructure> structureModel;
    private final @NotNull AsyncTreeModel treeModel;
    @Getter
    private final @NotNull SimpleTree mainTree;
    private final @NotNull AtomicBoolean refreshScheduled = new AtomicBoolean();

    private volatile @NotNull String expandedProjectPath = "";
    private volatile boolean disposed;

    private @NotNull Optional<Path> revealAfterRebuild = Optional.empty();

    public TreePanelTree(final @NotNull Project p, final @NotNull TreePanel tp) {
        this.p = p;
        this.tp = tp;

        this.treeStructure = new TreePanelStructure(p, bound());
        this.structureModel = new StructureTreeModel<>(treeStructure, this);
        this.treeModel = new AsyncTreeModel(structureModel, this);
        // UC-INTERNAL-001, Rule-INTERNAL-002
        this.mainTree = new TestinTree(treeModel);
        this.scrollPane = new JBScrollPane(mainTree);

        mainTree.setRootVisible(true);
        mainTree.setShowsRootHandles(true);
        mainTree.setDropMode(DropMode.ON);
        mainTree.setAutoscrolls(true);

        final @NotNull Set<DirectoryDto> sharedCutNodes = new HashSet<>();
        mainTree.setCellRenderer(new TreeCellRenderer(sharedCutNodes));

        final @NotNull TreeTransferHandler transferHandler = new TreeTransferHandler(p, mainTree, sharedCutNodes, this::refresh, this::refreshAndReveal);
        mainTree.setTransferHandler(transferHandler);
        mainTree.setDragEnabled(true);

        final @NotNull TreeContextMenu treeContextMenu = new TreeContextMenu(p, mainTree);
        mainTree.addMouseListener(new TreeMouseListener(p, mainTree, treeContextMenu));
        treeContextMenu.registerShortcuts(mainTree, transferHandler);

        Declared.bindTo("Testin.Open", mainTree);

        Declared.bindTo("Testin.CopyNode", mainTree);
        Declared.bindTo("Testin.CutNode", mainTree);
        Declared.bindTo("Testin.PasteNode", mainTree);

        Declared.bindTo("Testin.RemoveNode", mainTree);

        quietSwingsOwnClipboard(mainTree);
    }

    // Rule-TREE-PANEL-006
    private static void quietSwingsOwnClipboard(final @NotNull JTree tree) {
        final @NotNull Action nothing = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
            }
        };

        for (final Action swingOwn : List.of(TransferHandler.getCutAction(), TransferHandler.getCopyAction(), TransferHandler.getPasteAction())) {
            tree.getActionMap().put(swingOwn.getValue(Action.NAME), nothing);
        }
    }

    public void reveal(final @NotNull Path target) {
        reveal(target, NOTHING_AFTER);
    }

    public void reveal(final @NotNull Path target, final @NotNull Runnable afterFound) {
        if (disposed) return;

        TreeUtil.promiseSelect(mainTree, (final @NotNull TreePath path) -> {
            final @NotNull Optional<Path> at = TreeValues.directoryAt(path).map(DirectoryDto::getPath);
            if (at.isEmpty()) return TreeVisitor.Action.CONTINUE;

            if (at.get().equals(target)) return TreeVisitor.Action.INTERRUPT;

            return target.startsWith(at.get()) ? TreeVisitor.Action.CONTINUE : TreeVisitor.Action.SKIP_CHILDREN;
        }).onSuccess(found -> ApplicationManager.getApplication().invokeLater(() -> {
            if (disposed) return;

            mainTree.scrollPathToVisible(found);
            afterFound.run();
        }));
    }

    public void focus() {
        mainTree.requestFocusInWindow();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    public void refreshAndReveal(final @NotNull Path target) {
        revealAfterRebuild = Optional.of(target);
        refresh();
    }

    public void refresh() {
        if (disposed || !refreshScheduled.compareAndSet(false, true)) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (disposed) return;
                final @NotNull Optional<TestProjectDirectoryDto> boundProject = bound();
                treeStructure.setSelectedProject(boundProject);

                final @NotNull String projectPath = boundProject.map(dir -> dir.getPath().toString()).orElse("");
                final boolean projectChanged = !projectPath.isEmpty() && !projectPath.equals(expandedProjectPath);
                expandedProjectPath = projectPath;

                final @NotNull TreeState shape = TreeState.createOn(mainTree);

                structureModel.invalidateAsync().thenRun(() -> {
                    if (disposed) return;

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (disposed) return;

                        if (projectChanged) TreeUtil.promiseExpandAll(mainTree);
                        else shape.applyTo(mainTree);

                        consumePendingReveal();
                    });
                });

                mainTree.revalidate();
                mainTree.repaint();
            } finally {
                refreshScheduled.set(false);
            }
        });
    }

    private void consumePendingReveal() {
        final @NotNull Optional<Path> target = revealAfterRebuild;
        revealAfterRebuild = Optional.empty();

        target.ifPresent(this::reveal);
    }

    public void updateNodes() {
        tp.refresh();
    }

    private @NotNull Optional<TestProjectDirectoryDto> bound() {
        return Services.getInstance(p, BoundTestProject.class).get();
    }

    public @NotNull JComponent getComponent() {
        return scrollPane;
    }

    @Override
    public void dispose() {
        disposed = true;
        mainTree.setModel(null);
        treeModel.dispose();
        structureModel.dispose();
    }
}
