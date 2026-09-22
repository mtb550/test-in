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

package org.testin.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.BranchSelector;
import org.testin.explorer.toolbar.RefreshAction;
import org.testin.explorer.tree.TreePanelTree;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.setting.SettingsConfigurable;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BindTestProjectDialog;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.CloneTestProject;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class TreePanel implements Disposable {
    private static final int INLINE_CHOICES = 6;
    private final @NotNull Project p;
    @Getter
    private final @NotNull JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
    // UC-TREE-PANEL-001, Rule-TREE-PANEL-097
    private final @NotNull JBPanel<?> treeView = new JBPanel<>(new BorderLayout());
    private final @NotNull BranchSelector branchSelector;

    @Getter
    private final @NotNull RefreshAction refreshAction;

    @Getter
    private final @NotNull TreePanelTree projectTree;
    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101
    private @NotNull Optional<Content> content = Optional.empty();
    private @NotNull Map<String, ProjectStatus> underRoot = Map.of();

    public TreePanel(final @NotNull Project p) {
        this.p = p;
        Logger.info("TreePanel.TreePanel()");

        refreshAction = new RefreshAction(p, this);
        branchSelector = new BranchSelector(p, this, bound());
        projectTree = new TreePanelTree(p, this);
        Disposer.register(this, projectTree);

        final @NotNull JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
        topBar.add(branchSelector.getComponent(), BorderLayout.SOUTH);

        treeView.add(topBar, BorderLayout.NORTH);
        treeView.add(projectTree.getComponent(), BorderLayout.CENTER);

        panel.add(treeView, BorderLayout.CENTER);

        refresh();
        refreshWhenIndexed();
    }

    private void refreshWhenIndexed() {
        if (!Services.getInstance(p, TestinRoot.class).isConfigured()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).awaitIndexing();

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!p.isDisposed()) refresh();
            });
        });
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-001
    public void refresh() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, ProjectStatus> listing = Services.getInstance(p, ProjectIndexer.class).testProjects();

            if (bindTheOnlyProject(listing)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!p.isDisposed()) reindex();
                });
                return;
            }

            final @NotNull Optional<TestProjectDirectoryDto> boundProject = bound();
            final @NotNull PanelState state = state(listing, boundProject);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;
                draw(state, boundProject);
            });
        });
    }

    public boolean showsTree() {
        return treeView.isVisible();
    }

    private void draw(final @NotNull PanelState state, final @NotNull Optional<TestProjectDirectoryDto> boundProject) {
        treeView.setVisible(boundProject.isPresent());
        aimTheTitleBar();
        panel.getEmptyText().clear();

        boundProject.ifPresentOrElse(this::showTree, () -> showWelcome(state));

        panel.revalidate();
        panel.repaint();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101
    public void showIn(final @NotNull Content shownIn) {
        content = Optional.of(shownIn);
        aimTheTitleBar();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101, Rule-TREE-PANEL-097
    private void aimTheTitleBar() {
        final @NotNull JComponent onScreen = treeView.isVisible() ? projectTree.getMainTree() : panel;
        content.ifPresent(shownIn -> shownIn.setPreferredFocusableComponent(onScreen));
    }

    // UC-TREE-PANEL-004, Rule-TREE-PANEL-106
    private void bindTo(final @NotNull String name) {
        Services.getInstance(p, BoundTestProject.class).choose(name);
        reindex();
    }

    private @NotNull Optional<TestProjectDirectoryDto> bound() {
        return Services.getInstance(p, BoundTestProject.class).get();
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-015
    private boolean bindTheOnlyProject(final @NotNull Map<String, ProjectStatus> projects) {
        final @NotNull BoundTestProject bound = Services.getInstance(p, BoundTestProject.class);
        if (bound.isNamed() || projects.size() != 1) return false;

        final @NotNull String only = projects.keySet().iterator().next();
        bound.choose(only);

        Logger.info("Chose the only test project under the root: " + only);
        return true;
    }

    private @NotNull PanelState state(final @NotNull Map<String, ProjectStatus> listing, final @NotNull Optional<TestProjectDirectoryDto> boundProject) {
        underRoot = listing;

        return PanelState.of(
                Services.getInstance(p, TestinRoot.class).isConfigured(),
                Services.getInstance(p, ProjectIndexer.class).isIndexed(),
                boundProject.isPresent(),
                Services.getInstance(p, BoundTestProject.class).isMissing(underRoot),
                Services.getInstance(p, BoundTestProject.class).cloneAddress().isPresent(),
                !underRoot.isEmpty());
    }

    // UC-TREE-PANEL-001
    private void showTree(final @NotNull TestProjectDirectoryDto tp) {
        Logger.info("TreePanel.refresh(): showing '" + tp.getName() + "'");

        projectTree.refresh();
        branchSelector.updateProject(Optional.of(tp));
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-106
    private void showWelcome(final @NotNull PanelState state) {
        final @NotNull StatusText emptyText = panel.getEmptyText();

        emptyText.setText(Bundle.message("welcome.title", Bundle.getPluginName()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendSecondaryText(Bundle.message("welcome.tagline"), StatusText.DEFAULT_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine(Bundle.message("welcome.by"), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine(Bundle.message("welcome.author"), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        final @NotNull BoundTestProject boundProject = Services.getInstance(p, BoundTestProject.class);

        switch (state) {
            case NO_ROOT -> offerSettings(emptyText);
            case READING -> sayItIsReading(emptyText);
            case CLONE_BOUND -> offerClone(emptyText, boundProject);
            case NO_PROJECTS -> offerFirstProject(emptyText);
            case CHOOSE -> offerChoice(emptyText, boundProject);

            case TREE -> Logger.warn("Welcome screen asked to draw a resolved project");
        }
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-118, Rule-TREE-PANEL-119
    private void sayItIsReading(final @NotNull StatusText emptyText) {
        emptyText.appendLine(Bundle.message("welcome.reading"), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
    }

    // UC-TREE-PANEL-001
    private void offerSettings(final @NotNull StatusText emptyText) {
        emptyText.appendLine(
                AllIcons.General.Settings,
                Bundle.message("settings.action.description"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class));
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-003
    private void offerClone(final @NotNull StatusText emptyText, final @NotNull BoundTestProject boundProject) {
        final @NotNull String url = boundProject.cloneAddress().orElse("");
        final @NotNull String clone = Bundle.message("welcome.clone", boundProject.name());

        emptyText.appendLine(Bundle.message("welcome.not.here", boundProject.name()),
                SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");

        // Rule-TREE-PANEL-104
        if (!OptionalPlugin.GIT.isAvailable()) {
            emptyText.appendLine(AllIcons.Vcs.Clone, OptionalPlugin.GIT.needs(clone), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        } else {
            emptyText.appendLine(AllIcons.Vcs.Clone, clone, SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> new CloneTestProject(p, url, boundProject.name(), this).execute());
        }

        emptyText.appendLine("");

        if (underRoot.isEmpty()) {
            offerFirstProject(emptyText);
            return;
        }

        emptyText.appendLine(
                AllIcons.Actions.ModuleDirectory,
                Bundle.message("welcome.another.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new BindTestProjectDialog(p, underRoot, this::reindex).show());
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-002
    private void offerFirstProject(final @NotNull StatusText emptyText) {
        emptyText.appendLine(
                AllIcons.General.Add,
                Bundle.message("welcome.first.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new CreateTestProjectAction(p, this).execute());
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-004
    private void offerChoice(final @NotNull StatusText emptyText, final @NotNull BoundTestProject boundProject) {
        final @NotNull String problem = boundProject.problem(underRoot);
        if (!problem.isEmpty()) {
            emptyText.appendLine(problem, SimpleTextAttributes.ERROR_ATTRIBUTES, null);
            emptyText.appendLine("");
        }

        if (underRoot.size() <= INLINE_CHOICES) {
            underRoot.forEach((name, status) -> emptyText.appendLine(
                    AllIcons.Actions.ModuleDirectory,
                    name + "  " + status.getLabel(),
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> bindTo(name)));
            return;
        }

        emptyText.appendLine(
                AllIcons.Actions.ModuleDirectory,
                Bundle.message("welcome.select.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new BindTestProjectDialog(p, underRoot, this::reindex).show());
    }

    public void reindex() {
        refreshAction.execute();
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-108
    public void fetchBranches() {
        branchSelector.fetchBranches();
    }

    public void reindex(final @NotNull String outcome) {
        refreshAction.execute(outcome);
    }

    @Override
    public void dispose() {
    }
}
