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
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinConfigService;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.RefreshAction;
import org.testin.explorer.tree.ExplorerTree;
import org.testin.explorer.toolbar.BranchSelector;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;
import org.testin.setting.SettingsConfigurable;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BindTestProjectDialog;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.CreateTestProjectCloneAction;
import org.testin.util.Bundle;

import java.awt.*;
import java.util.Optional;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class ExplorerPanel implements Disposable {
    private final @NotNull Project p;

    /**
     * The component the tool window shows.
     */
    @Getter
    private final @NotNull JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());

    private final @NotNull BranchSelector branchSelector;

    /**
     * Asked for by every action that changes a node and has to redraw it.
     */
    @Getter
    private final @NotNull ExplorerTree projectTree;

    /**
     * What is under the Testin root, as the last draw read it. Held for the one
     * hop between deciding which state to show and drawing it, so the listing is
     * not walked twice for the same picture.
     */
    private @NotNull Map<String, ProjectStatus> underRoot = Map.of();

    /**
     * How many projects the welcome screen offers as lines before it hands the
     * choice to the picker instead. A status text does not scroll, so a long
     * list would run off the panel.
     */
    private static final int INLINE_CHOICES = 6;

    public ExplorerPanel(final @NotNull Project p) {
        this.p = p;
        Logger.info("ExplorerPanel.ExplorerPanel()");

        branchSelector = new BranchSelector(p, this, bound());
        projectTree = new ExplorerTree(p, this);
        Disposer.register(this, projectTree);

        refresh();
        refreshWhenIndexed();
    }

    /**
     * Draws again when indexing finishes.
     * <p>
     * The panel is built the moment the tool window is opened, and on a cold
     * start that is while the index is still being built. The bound project is
     * looked up in the index, so drawing only once would show a bound repository
     * the screen for an unbound one - and leave it there.
     */
    private void refreshWhenIndexed() {
        // Nothing indexes without a root, so the wait would never end - and a
        // root configured later comes back through Apply, which refreshes every
        // open panel itself.
        if (Services.getInstance(p, TestinRoot.class).getPath().toString().isEmpty()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).awaitIndexing();

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!p.isDisposed()) refresh();
            });
        });
    }

    /**
     * Redraws the panel around whichever test project this repository is bound
     * to - the tree when there is one, and the way to get one when there is not.
     * <p>
     * The one owner of what the panel shows. Every action that can change the
     * answer calls this and nothing else - indexing finishing, a refresh, a
     * rename, creating or cloning a project, binding one. The tree, the branch
     * box and the empty state can then never disagree about which project is open.
     */
    public void refresh() {
        // Gathered off the EDT, drawn on it. What the panel decides on is a
        // directory walk that reads a marker per project, and the threading rule
        // in CLAUDE.md keeps disk work off the thread that paints (#66).
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, ProjectStatus> listing = Services.getInstance(p, ProjectIndexer.class).testProjects();

            // Binding changes what indexing covers, so the answer is re-indexed
            // rather than redrawn - the same route every other binder takes.
            if (bindTheOnlyProject(listing)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!p.isDisposed()) reindex();
                });
                return;
            }

            final @NotNull PanelState state = state(listing);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;
                draw(state);
            });
        });
    }

    /**
     * Draws the panel from an answer it was given. On the EDT, and reading
     * nothing: every question it could ask was answered by
     * {@link #state(Map)} before it was called.
     */
    private void draw(final @NotNull PanelState state) {
        panel.removeAll();
        panel.getEmptyText().clear();

        bound().ifPresentOrElse(this::showTree, () -> showWelcome(state));

        panel.revalidate();
        panel.repaint();
    }

    /**
     * Binds to a project the tester clicked in the welcome screen, off the EDT
     * because it writes {@code testin.yml}, and redraws either way - a write
     * that failed has said so, and the screen must not sit there looking as
     * though the click did nothing.
     */
    private void bindTo(final @NotNull String name) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final boolean bound = Services.getInstance(p, BoundTestProject.class).bind(name);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;

                // Re-indexed rather than redrawn: indexing is scoped to the
                // bound project, so the cache built before the binding is not
                // the one the tree needs.
                if (bound) reindex();
                else refresh();
            });
        });
    }

    private @NotNull Optional<TestProjectDirectoryDto> bound() {
        return Services.getInstance(p, BoundTestProject.class).get();
    }

    /**
     * Binds a repository that names no test project to the only one there is,
     * and says whether it did.
     * <p>
     * A picker with one row is a question with one answer, and a fresh clone of
     * an automation repository beside a Testin root that holds a single project
     * is the common first run. It writes the binding and the tree opens, rather
     * than asking a tester who has nothing to choose between.
     * <p>
     * Only when the repository names nothing at all. A name that resolves to
     * nothing - a renamed folder, an archived project - is a different state
     * with a different sentence, and silently rebinding it would hide the thing
     * the tester needs to know (#8).
     * <p>
     * On the pooled thread that gathers, because it writes {@code testin.yml};
     * the listing it decides from is the one that pass already read.
     */
    private boolean bindTheOnlyProject(final @NotNull Map<String, ProjectStatus> projects) {
        final @NotNull BoundTestProject bound = Services.getInstance(p, BoundTestProject.class);
        if (bound.isNamed() || projects.size() != 1) return false;

        final @NotNull String only = projects.keySet().iterator().next();
        if (!bound.bind(only)) return false;

        Logger.info("Bound to the only test project under the root: " + only);
        return true;
    }

    /**
     * The five facts the panel decides on, gathered here and answered by
     * {@link PanelState}. The root and the project listing are disk reads, so
     * they are asked for once per draw rather than once per branch.
     */
    private @NotNull PanelState state(final @NotNull Map<String, ProjectStatus> listing) {
        // Handed in rather than read here: the caller has already walked the root
        // to decide whether there was one project to bind to, and that walk reads
        // a marker per project.
        underRoot = listing;

        return PanelState.of(
                !Services.getInstance(p, TestinRoot.class).getPath().toString().isEmpty(),
                bound().isPresent(),
                Services.getInstance(p, BoundTestProject.class).isMissing(underRoot),
                Services.getInstance(p, TestinConfigService.class).get().hasRepoUrl(),
                !underRoot.isEmpty());
    }

    private void showTree(final @NotNull TestProjectDirectoryDto tp) {
        Logger.info("ExplorerPanel.refresh(): showing '" + tp.getName() + "'");

        panel.setLayout(new BorderLayout());

        final @NotNull JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
        topBar.add(branchSelector.getComponent(), BorderLayout.SOUTH);
        panel.add(topBar, BorderLayout.NORTH);

        panel.add(projectTree.getComponent(), BorderLayout.CENTER);

        projectTree.refresh();
        branchSelector.updateProject(Optional.of(tp));
    }

    /**
     * The screen for a repository with no project open, and the one step out of
     * it. Which step depends on what is missing: a root to look in, a project to
     * look at, or the line in {@code testin.yml} that says which one (#8).
     */
    private void showWelcome(final @NotNull PanelState state) {
        final @NotNull StatusText emptyText = panel.getEmptyText();

        emptyText.setText(String.format("Welcome to %s", Bundle.getPluginName()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendSecondaryText("The new awesome test management tool", StatusText.DEFAULT_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("By", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("Muteb almughyiri", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        final @NotNull BoundTestProject boundProject = Services.getInstance(p, BoundTestProject.class);

        switch (state) {
            case NO_ROOT -> emptyText.appendLine(
                    AllIcons.General.Settings,
                    "Configure Testin settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class));

            case CLONE_BOUND -> {
                final @NotNull String url = Services.getInstance(p, TestinConfigService.class).get().testinRepoUrl();

                emptyText.appendLine(boundProject.name() + " is not on this machine yet",
                        SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
                emptyText.appendLine("");
                emptyText.appendLine(
                        AllIcons.Vcs.Clone,
                        "Clone " + boundProject.name(),
                        SimpleTextAttributes.LINK_ATTRIBUTES,
                        e -> new CreateTestProjectCloneAction(p, url, boundProject.name(), this).execute());
            }

            case NO_PROJECTS -> emptyText.appendLine(
                    AllIcons.General.Add,
                    "Create your first test project",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> new CreateTestProjectAction(p, this).execute());

            case CHOOSE -> {
                // Say why before offering the picker, so a binding that stopped
                // resolving - a renamed folder, an archived project - reads as a
                // fact and not as a first run.
                final @NotNull String problem = boundProject.problem(underRoot);
                if (!problem.isEmpty()) {
                    emptyText.appendLine(problem, SimpleTextAttributes.ERROR_ATTRIBUTES, null);
                    emptyText.appendLine("");
                }

                // Few enough to read at a glance: one line each, one click to
                // bind. The dialog is for the root that holds more than a
                // screenful, where a list in a status text stops being a list.
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
                        "Select the test project for this repository",
                        SimpleTextAttributes.LINK_ATTRIBUTES,
                        e -> new BindTestProjectDialog(p, underRoot, this::reindex).show());
            }

            // Unreachable: the tree is drawn by showTree, and this method is only
            // called when there is no project to draw.
            case TREE -> Logger.warn("Welcome screen asked to draw a resolved project");
        }
    }

    /**
     * Indexes again and redraws. Binding a repository changes which project is
     * indexed at all, so the cache built for the old answer is not the one the
     * tree needs - which is why every caller that binds one comes back here
     * rather than calling {@link #refresh()} directly.
     */
    public void reindex() {
        new RefreshAction(p, this).execute();
    }

    /**
     * Re-indexes and rebuilds, reporting what caused it rather than the generic
     * refresh - a branch switch says which branch.
     */
    public void reindex(final @NotNull String outcome) {
        new RefreshAction(p, this).execute(outcome);
    }

    @Override
    public void dispose() {
    }
}
