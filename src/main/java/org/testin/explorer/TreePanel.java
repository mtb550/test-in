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
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import java.awt.BorderLayout;
import com.intellij.ui.components.JBPanelWithEmptyText;
import org.testin.git.ClonedFrom;
import org.testin.config.TestinConfigService;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.RefreshAction;
import org.testin.explorer.tree.TreePanelTree;
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
public final class TreePanel implements Disposable {
    private final @NotNull Project p;

    /**
     * The component the tool window shows.
     */
    @Getter
    private final @NotNull JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());

    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-097.
     * <p>
     * The branch bar and the tree, added to the panel once and hidden rather
     * than removed when there is no project to show.
     * <p>
     * <b>The tree may never leave the component hierarchy.</b> The content's
     * preferred focusable component is the tree, and the platform anchors the
     * data context for every title-bar button on exactly that component -
     * {@code ToolWindowHeader}'s toolbar asks the selected content for it before
     * each press. A component with no parent has no frame above it, so that
     * context cannot answer which project it belongs to, and the platform reads
     * {@code event.project!!} before any title action runs.
     * <p>
     * The panel used to be emptied and rebuilt on every draw, so archiving the
     * bound test project took the tree out of the hierarchy - and the next press
     * on Settings threw a NullPointerException out of the platform, from a stack
     * with no Testin frame in it (#66).
     */
    private final @NotNull JBPanel<?> treeView = new JBPanel<>(new BorderLayout());

    private final @NotNull BranchSelector branchSelector;

    /**
     * The one Refresh for this project, held here because the guard that stops
     * two re-indexes overlapping is a field on it.
     * <p>
     * Every caller used to build its own, so the guard only ever stopped a
     * second click on the same toolbar button. A branch switch landing while
     * the tester pressed Refresh started a second re-index through the first:
     * it wipes the cache the first pass is filling and replaces the latch the
     * other pass counts down, so the tree can be drawn from a half-built index.
     * The "already in progress, ignoring click" line could never appear for the
     * one combination it was written for.
     */
    @Getter
    private final @NotNull RefreshAction refreshAction;

    /**
     * Asked for by every action that changes a node and has to redraw it.
     */
    @Getter
    private final @NotNull TreePanelTree projectTree;

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
        if (!Services.getInstance(p, TestinRoot.class).isConfigured()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).awaitIndexing();

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!p.isDisposed()) refresh();
            });
        });
    }

    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-001.
     * <p>
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

            // Read once, on this thread, and carried to the draw. The draw
            // used to ask again on the EDT a moment later, so a concurrent
            // refresh emptying the index cache between the two reads left the
            // state saying TREE while the second read said nothing was bound -
            // and the welcome screen's TREE branch appends no links at all. The
            // tester got the "Welcome to Testin" header with no Create, Clone or
            // Select line, and no way out but pressing Refresh again.
            final @NotNull Optional<TestProjectDirectoryDto> boundProject = bound();
            final @NotNull PanelState state = state(listing, boundProject);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;
                draw(state, boundProject);
            });
        });
    }

    /**
     * Draws the panel from an answer it was given. On the EDT, and reading
     * nothing: every question it could ask was answered by
     * {@link #state(Map, Optional)} before it was called - the bound project
     * included, which is the one it used to go and read for itself.
     */
    private void draw(final @NotNull PanelState state, final @NotNull Optional<TestProjectDirectoryDto> boundProject) {
        // Hidden, not removed - see the field. What the welcome screen replaces
        // is what the panel draws, not what it contains.
        treeView.setVisible(boundProject.isPresent());
        panel.getEmptyText().clear();

        boundProject.ifPresentOrElse(this::showTree, () -> showWelcome(state));

        panel.revalidate();
        panel.repaint();
    }

    /**
     * UC-TREE-PANEL-004, Rule-TREE-PANEL-020.
     * <p>
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
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-015.
     * <p>
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
        // Never over a file that would not parse. Binding writes a testinProject
        // line into it, so a mistyped indent used to be answered by writing into
        // the broken file - on every open, still broken, and never said out loud
        // (#66, finding 10).
        if (Services.getInstance(p, TestinConfigService.class).get().isUnreadable()) return false;

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
    private @NotNull PanelState state(final @NotNull Map<String, ProjectStatus> listing, final @NotNull Optional<TestProjectDirectoryDto> boundProject) {
        // Handed in rather than read here: the caller has already walked the root
        // to decide whether there was one project to bind to, and that walk reads
        // a marker per project. The bound project comes in for the same reason
        // and one more - the draw forks on it, so the two must be one read.
        underRoot = listing;

        return PanelState.of(
                Services.getInstance(p, TestinRoot.class).isConfigured(),
                Services.getInstance(p, TestinConfigService.class).get().isUnreadable(),
                boundProject.isPresent(),
                Services.getInstance(p, BoundTestProject.class).isMissing(underRoot),
                Services.getInstance(p, TestinConfigService.class).get().hasRepoUrl(),
                !underRoot.isEmpty());
    }

    // UC-TREE-PANEL-001
    private void showTree(final @NotNull TestProjectDirectoryDto tp) {
        Logger.info("TreePanel.refresh(): showing '" + tp.getName() + "'");

        projectTree.refresh();
        branchSelector.updateProject(Optional.of(tp));
        ClonedFrom.record(p, tp.getPath());
    }


    /**
     * UC-TREE-PANEL-001.
     * <p>
     * The screen for a repository with no project open, and the one step out of
     * it. Which step depends on what is missing: a root to look in, a project to
     * look at, or the line in {@code testin.yml} that says which one (#8).
     */
    private void showWelcome(final @NotNull PanelState state) {
        final @NotNull StatusText emptyText = panel.getEmptyText();

        emptyText.setText(Bundle.message("welcome.title", Bundle.getPluginName()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendSecondaryText(Bundle.message("welcome.tagline"), StatusText.DEFAULT_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine(Bundle.message("welcome.by"), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("Muteb almughyiri", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        final @NotNull BoundTestProject boundProject = Services.getInstance(p, BoundTestProject.class);

        // Which offer to make is the state's answer; making it is this panel's
        // job, because every branch reaches back into it - to bind a project, to
        // open settings, to index again.
        switch (state) {
            case NO_ROOT -> offerSettings(emptyText);
            case CLONE_BOUND -> offerClone(emptyText, boundProject);
            case BROKEN_CONFIG -> sayTheFileIsBroken(emptyText);
            case NO_PROJECTS -> offerFirstProject(emptyText);
            case CHOOSE -> offerChoice(emptyText, boundProject);

            // Unreachable, and now actually so: the state and the branch that
            // chose this method come from one read of the bound project, so
            // TREE here would mean the two disagreed about a single value.
            case TREE -> Logger.warn("Welcome screen asked to draw a resolved project");
        }
    }

    /**
     * UC-TREE-PANEL-001.
     * <p>
     * No root is set, so the only step out of here is the settings page.
     */
    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-002.
     * <p>
     * The repository has a {@code testin.yml} and it could not be read.
     * <p>
     * Named rather than offered a way out, because there is no button that
     * corrects a file: the tester opens it and fixes the line. What the plugin
     * can do is say which file and that the reason is in the log, instead of
     * reporting the same "not bound to a test project" an unbound repository
     * gets - which sent the tester to the picker to fix something the picker
     * cannot reach (#66, finding 10).
     */
    private void sayTheFileIsBroken(final @NotNull StatusText emptyText) {
        emptyText.appendLine(Bundle.message("welcome.config.broken", TestinConfigService.fileName()),
                SimpleTextAttributes.ERROR_ATTRIBUTES, null);
        emptyText.appendLine(Bundle.message("welcome.config.broken.detail"),
                SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
    }

    private void offerSettings(final @NotNull StatusText emptyText) {
        emptyText.appendLine(
                AllIcons.General.Settings,
                Bundle.message("settings.action.description"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class));
    }

    /**
     * UC-TREE-PANEL-001, UC-TREE-PANEL-003.
     * <p>
     * The repository names a project this machine does not hold yet, so the step
     * out is to clone the one it names rather than to pick a different one.
     */
    private void offerClone(final @NotNull StatusText emptyText, final @NotNull BoundTestProject boundProject) {
        final @NotNull String url = Services.getInstance(p, TestinConfigService.class).get().repoUrl();

        emptyText.appendLine(Bundle.message("welcome.not.here", boundProject.name()),
                SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine(
                AllIcons.Vcs.Clone,
                Bundle.message("welcome.clone", boundProject.name()),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new CreateTestProjectCloneAction(p, url, boundProject.name(), this).execute());
    }

    /**
     * UC-TREE-PANEL-001, UC-TREE-PANEL-002.
     * <p>
     * The root is set and empty, so there is nothing to choose between yet.
     */
    private void offerFirstProject(final @NotNull StatusText emptyText) {
        emptyText.appendLine(
                AllIcons.General.Add,
                Bundle.message("welcome.first.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new CreateTestProjectAction(p, this).execute());
    }

    /**
     * UC-TREE-PANEL-001, UC-TREE-PANEL-004.
     * <p>
     * The root holds projects and none of them is bound to this repository, so
     * the step out is to say which.
     */
    private void offerChoice(final @NotNull StatusText emptyText, final @NotNull BoundTestProject boundProject) {
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
                Bundle.message("welcome.select.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new BindTestProjectDialog(p, underRoot, this::reindex).show());
    }

    /**
     * Indexes again and redraws. Binding a repository changes which project is
     * indexed at all, so the cache built for the old answer is not the one the
     * tree needs - which is why every caller that binds one comes back here
     * rather than calling {@link #refresh()} directly.
     */
    public void reindex() {
        refreshAction.execute();
    }

    /**
     * Re-indexes and rebuilds, reporting what caused it rather than the generic
     * refresh - a branch switch says which branch.
     */
    public void reindex(final @NotNull String outcome) {
        refreshAction.execute(outcome);
    }

    @Override
    public void dispose() {
    }
}
