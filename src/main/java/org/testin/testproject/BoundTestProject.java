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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
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
     */
    static @NotNull String resolve(final @NotNull String inFile, final @NotNull List<String> choice) {
        return choice.size() == 2 && choice.get(1).equals(inFile) ? choice.getFirst() : inFile;
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
        final boolean fromTheFile = name.equals(TestinYml.projectName(p));

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
    }

    /**
     * UC-TREE-PANEL-001, UC-TREE-PANEL-003.
     * <p>
     * Where the project this repository is about can be cloned from: the file's
     * {@code RepoUrl}, and only while the project is the one the file names. The
     * address is the file's project's, so a tester's own pick of another project
     * that is missing here is not offered it - that cloned the file's repository
     * into the pick's folder (#301, R7).
     */
    public @NotNull Optional<String> cloneAddress() {
        if (!TestinYml.hasRepoUrl(p) || !TestinYml.projectName(p).equals(name())) return Optional.empty();

        return Optional.of(TestinYml.repoUrl(p));
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
