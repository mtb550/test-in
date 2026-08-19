package org.testin.config;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.services.Services;

import java.nio.file.Path;

/**
 * What this automation repository says about its test project, read once and kept
 * (#6).
 * <p>
 * A plain project service and deliberately not a {@code PersistentStateComponent}:
 * the file is input the repository owns, not state the IDE stores. Nothing is
 * written back on exit, and two machines opening the same clone read the same
 * answer.
 * <p>
 * Written only from an action a tester took - binding the repository to a test
 * project, or accepting a clone URL - and one key at a time, so the comments and
 * every other line in the file survive.
 */
@Service(Service.Level.PROJECT)
public final class TestinConfigService {

    private final @NotNull Project p;
    private volatile @NotNull TestinProjectConfig config;

    public TestinConfigService(final @NotNull Project p) {
        this.p = p;
        this.config = TestinConfigLoader.load(p);
    }

    public @NotNull TestinProjectConfig get() {
        return config;
    }

    /**
     * Reads the file again - after a branch switch brought a different revision
     * of it, or after a tester edited it by hand.
     */
    public void reload() {
        config = TestinConfigLoader.load(p);
    }

    /**
     * Records which test project this repository exercises, so the next clone of
     * it needs no setting up.
     */
    public boolean bind(final @NotNull String testinProject) {
        return set("testinProject", testinProject);
    }

    /**
     * Records where the test project is cloned from, so the tester who typed it
     * at the push prompt is the last one who has to.
     * <p>
     * Nothing is reported back. The push carries on with the URL either way, and
     * a file that could not be written has already said so in the log - a second
     * message about it, mid-push, would be about the wrong thing.
     */
    public void rememberRepoUrl(final @NotNull String url) {
        set("testinRepoUrl", url);
    }

    /**
     * One key into the file, then the file back into memory - so what the service
     * reports is what the file says rather than what the caller hoped, including
     * for a value the file refused to keep.
     * <p>
     * Answers whether the file now says it: a repository with no base path, or a
     * disk that refuses, leaves the tester set up for this session and back where
     * they started on the next open, which the caller has to be able to say out
     * loud rather than report as done.
     */
    private boolean set(final @NotNull String key, final @NotNull String value) {
        final Path file = TestinConfigLoader.file(p);
        if (file == null) {
            Logger.warn("No base path for " + p.getName() + ", so " + key + " cannot be written");
            return false;
        }

        if (!TestinConfigWriter.write(file, key, value)) return false;

        // The file was written with java.nio, so until the VFS is told, the
        // Project view shows a repository that does not have a testin.yml - and
        // the tester's next move is to commit the file they cannot see.
        Services.getInstance(p, ProjectIndexer.class).refreshFile(file);

        reload();
        return true;
    }
}
