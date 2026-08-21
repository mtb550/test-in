package org.testin.testproject;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinConfigService;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;

import java.util.Optional;
import java.util.Map;

/**
 * The one test project this automation repository is about (#8).
 * <p>
 * There used to be a combo box, and ten places read the answer out of it - the
 * tree, the branch box, the run creator, two report generators. The answer never
 * belonged to a Swing component: it belongs to the repository, which says so in
 * its {@code testin.yml} (#6). This is the one place that turns that name into a
 * project, so a caller asks what the repository is about rather than what a
 * dropdown currently shows.
 * <p>
 * Nothing is cached. The name comes from a config read once, and the lookup is
 * over the indexer's own map - so the answer is never stale, and it starts being
 * right the moment indexing puts the project there.
 */
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class BoundTestProject {

    private final @NotNull Project p;

    /**
     * The name the repository gives, empty when it gives none. What the file says,
     * even when no project by that name is there - which is what the message for a
     * name that resolves to nothing has to show.
     */
    public @NotNull String name() {
        return Services.getInstance(p, TestinConfigService.class).get().testinProject();
    }

    /**
     * Whether the repository names a test project at all. False is the state a
     * tester is guided out of, once, by picking one.
     */
    public boolean isNamed() {
        return !name().isEmpty();
    }

    /**
     * The named project as the indexer holds it, empty when the name matches
     * nothing there - no file, a name nobody uses, or a project that is archived
     * and therefore never indexed.
     * <p>
     * Matched on the folder name, which is what a test project is identified by
     * everywhere else. Renaming the folder breaks the binding, and the tester is
     * sent back to the picker rather than shown a wrong project.
     */
    public @NotNull Optional<TestProjectDirectoryDto> get() {
        final String name = name();
        if (name.isEmpty()) return Optional.empty();

        return Services.getInstance(p, ProjectIndexer.class).getTestProjectsByPath().values().stream()
                .filter(tp -> name.equals(tp.getName()))
                .findFirst();
    }

    /**
     * Whether the named project is nowhere under the Testin root.
     * <p>
     * The case the config file exists for: a machine that has the automation
     * repository and not the test data. Worth telling apart from every other
     * reason a binding does not resolve, because it is the one a clone fixes.
     *
     * @param underRoot what is under the Testin root, by name - taken as an
     *                  argument because it is a directory read, and the caller
     *                  that draws the panel needs the same listing for its own
     *                  decision
     */
    public boolean isMissing(final @NotNull Map<String, ProjectStatus> underRoot) {
        return isNamed() && !underRoot.containsKey(name());
    }

    /**
     * Why the named project is not showing, in one sentence a tester can act on,
     * or empty when there is nothing wrong. Archived is called by its name
     * because it is the one cause with an obvious fix.
     */
    public @NotNull String problem(final @NotNull Map<String, ProjectStatus> underRoot) {
        if (!isNamed() || get().isPresent()) return "";

        final String name = name();
        final Optional<ProjectStatus> status = Optional.ofNullable(underRoot.get(name));

        if (status.isEmpty()) return "testin.yml names " + name + ", which is not under the Testin root";
        if (status.orElseThrow() == ProjectStatus.ARCHIVED) return name + " is archived, so it is not opened";
        return "testin.yml names " + name + ", which could not be read";
    }

    /**
     * Binds the repository to a project, by writing the name into its
     * {@code testin.yml}. Answers whether the file now says so - a tester who is
     * told the binding is done and finds it gone on the next open is worse off
     * than one who is told it could not be written.
     */
    public boolean bind(final @NotNull String projectName) {
        Logger.info("Binding " + p.getName() + " to test project '" + projectName + "'");
        return Services.getInstance(p, TestinConfigService.class).bind(projectName);
    }
}
