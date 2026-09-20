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

package org.testin.testproject;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.git.GitRefs;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The one test project this automation repository is about (#8).
 * <p>
 * There used to be a combo box, and ten places read the answer out of it - the
 * tree, the branch box, the run creator, two report generators. The answer never
 * belonged to a Swing component, so a caller asks here what the repository is
 * about rather than what a dropdown currently shows.
 * <p>
 * Two things can name it, and this is the one place that weighs them: the
 * project {@code testin.yml} names, and the one the tester chose on this
 * machine. Nothing is written into the code project; the choice is kept in the
 * IDE's own storage for this project, which stays on this machine
 * (Rule-TREE-PANEL-106).
 */
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class BoundTestProject {

    /**
     * The chosen project and the name {@code testin.yml} gave when it was chosen,
     * as one value so the two are never read half-written.
     */
    private static final @NotNull String CHOICE = "testin.chosenTestProject";

    private final @NotNull Project p;

    /**
     * UC-TREE-PANEL-004, Rule-TREE-PANEL-106.
     * <p>
     * The name this repository is about, empty when nothing names one. What the
     * name says, even when no project by that name is there - which is what the
     * message for a name that resolves to nothing has to show.
     */
    public @NotNull String name() {
        return resolve(TestinYml.projectName(p), choice());
    }

    /**
     * Rule-TREE-PANEL-106.
     * <p>
     * The choice wins while {@code testin.yml} still names what it named when the
     * choice was made; once the file names something else - a pull brought a
     * colleague's change - the file's name is the answer again. So the tester is
     * never held by the file, and the team's change still arrives.
     * <p>
     * <b>A file that names nothing names no different project</b>, so the choice
     * stands (#66, finding 316). It used to be thrown away: taking
     * {@code testinProject} out of the file, or leaving a file that would not
     * parse, made {@code inFile} empty, which matched no stored name - and a
     * tester who had picked a project was put back on the choose screen by an
     * edit that said nothing about their pick. It is also what lets an unreadable
     * file behave exactly like an absent one (Rule-TREE-PANEL-119).
     */
    static @NotNull String resolve(final @NotNull String inFile, final @NotNull List<String> choice) {
        if (choice.size() != 2) return inFile;

        return inFile.isEmpty() || choice.get(1).equals(inFile) ? choice.getFirst() : inFile;
    }

    /**
     * Whether anything names a test project at all. False is the state a tester
     * is guided out of, once, by picking one.
     */
    public boolean isNamed() {
        return !name().isEmpty();
    }

    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-001.
     * <p>
     * The named project as the indexer holds it, empty when the name matches
     * nothing there. A project that is not active resolves like any other: it is
     * indexed as a node, and only its contents are left unread (#66).
     * <p>
     * Matched on the folder name, which is what a test project is identified by
     * everywhere else.
     */
    public @NotNull Optional<TestProjectDirectoryDto> get() {
        final @NotNull String name = name();
        if (name.isEmpty()) return Optional.empty();

        return Services.getInstance(p, ProjectIndexer.class).getTestProjectsByPath().values().stream()
                .filter(tp -> name.equals(tp.getName()))
                .findFirst();
    }

    /**
     * Whether the named project is nowhere in the Testin folder.
     * <p>
     * The case a clone fixes, and worth telling apart from every other reason a
     * name does not resolve for that reason.
     *
     * @param underRoot what is in the Testin folder, by name - taken as an
     *                  argument because it is a directory read, and the caller
     *                  that draws the panel needs the same listing for its own
     *                  decision
     */
    public boolean isMissing(final @NotNull Map<String, ProjectStatus> underRoot) {
        return isNamed() && !underRoot.containsKey(name());
    }

    /**
     * UC-TREE-PANEL-001.
     * <p>
     * Why the named project is not showing, in one sentence a tester can act on,
     * or empty when there is nothing wrong. It says where the name came from -
     * the file, or this machine's choice - because that is where it is changed.
     */
    public @NotNull String problem(final @NotNull Map<String, ProjectStatus> underRoot) {
        if (!isNamed() || get().isPresent()) return "";

        final @NotNull String name = name();
        final boolean missing = !underRoot.containsKey(name);
        final boolean fromTheFile = TestinYml.names(p, name);

        if (fromTheFile) return missing ? Bundle.message("bound.not.under.root", name) : Bundle.message("bound.unreadable", name);

        return missing ? Bundle.message("chosen.not.under.root", name) : Bundle.message("chosen.unreadable", name);
    }

    /**
     * UC-TREE-PANEL-004, Rule-TREE-PANEL-106.
     * <p>
     * Makes this the project the repository is about, on this machine - the
     * picker, the welcome screen, creating a test project and cloning one all
     * choose through here. Kept with the name {@code testin.yml} gives right
     * now, which is what lets a later change to the file win again.
     */
    public void choose(final @NotNull String projectName) {
        Logger.info("Chose test project '" + projectName + "' for " + p.getName() + " on this machine");

        PropertiesComponent.getInstance(p).setList(CHOICE, List.of(projectName, TestinYml.projectName(p)));
        refreshGutter();
    }

    /**
     * Rule-CODEGEN-082.
     * <p>
     * Reads {@code testin.yml} again - Refresh, or Report Bug about to send -
     * and lets the gutter answer again, since what the file names is half of
     * whether code is on.
     */
    public void reread() {
        TestinYml.reload(p);
        refreshGutter();
    }

    /**
     * Rule-CODEGEN-082.
     * <p>
     * Draws the Java gutter again. Its run icons show only while code is on, and
     * that is weighed here - the project this repository is about, and whether
     * {@code testin.yml} names it - so every change to either redraws it. Only
     * Save to testin.yml used to: after choosing another project, a rename, a
     * clone or a Refresh, the icons stayed as they were until the file was
     * edited (#66, finding 312).
     */
    public void refreshGutter() {
        // With a reason: the bare restart() is deprecated, and Build fails on a
        // deprecated call (#324). The reason only reaches the IDE's diagnostics.
        ApplicationManager.getApplication().invokeLater(() ->
                DaemonCodeAnalyzer.getInstance(p).restart("Whether Testin's code is on may have changed"), p.getDisposed());
    }

    /**
     * UC-TREE-PANEL-001, UC-TREE-PANEL-003.
     * <p>
     * Where the project this repository is about can be cloned from, and empty
     * for a tester's own pick of a project the file does not name - offering it
     * the file's address cloned the file's repository into the pick's folder
     * (#301, R7).
     */
    public @NotNull Optional<String> cloneAddress() {
        return TestinYml.cloneAddress(p, name()).filter(GitRefs::isRepositoryUrl);
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-110.
     * <p>
     * The project this repository is about was renamed, so the choice follows
     * it - whatever {@code testin.yml} names, which a rename never writes. Only
     * when it was this repository's project: undoing a rename after choosing
     * another one must not take the tester back to it.
     */
    public void follow(final @NotNull String oldName, final @NotNull String newName) {
        if (name().equals(oldName)) choose(newName);
    }

    private @NotNull List<String> choice() {
        return Optional.ofNullable(PropertiesComponent.getInstance(p).getList(CHOICE)).orElse(List.of());
    }
}
