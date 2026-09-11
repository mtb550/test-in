package org.testin.indexer;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.DirectoryMapper;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * UC-INTERNAL-002.
 * <p>
 * The cache and the disk agree after a tree operation.
 * <p>
 * <b>Architecture rule 2:</b> the VFS operation succeeds first, then the cache
 * is updated - never the other way round, because the cache update persists
 * markers, marker writes create directories, and the reverse order produces
 * phantom directories and "already exists in VFS" errors. Nothing has ever
 * checked it.
 * <p>
 * This is the first test in this repository that needs a running IDE, and the
 * reason the {@code ideTest} task exists (#108). The indexer is a project
 * service over the virtual file system; there is no seam that answers these
 * questions without one, and three separate pieces of work wanted such a fixture
 * in a single session.
 * <p>
 * Named {@code *IdeTest} because that is how the two runners tell the tests
 * apart: {@code test} is TestNG and excludes this, {@code ideTest} is JUnit and
 * takes only this. A {@code BasePlatformTestCase} is a JUnit {@code TestCase},
 * which the TestNG runner would never have seen.
 */
public class TreeOperationsIdeTest extends BasePlatformTestCase {

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-tree");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

    /**
     * A temporary tree, removed as far as the operating system allows. A file
     * the IDE still holds open is its own to clean up, and failing to remove one
     * fails nothing here.
     */
    private static void deleteTree(final Path path) {
        if (path == null) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> {
                try {
                    Files.deleteIfExists(each);
                } catch (final Exception ignored) {
                    // Left for the operating system.
                }
            });
        } catch (final Exception ignored) {
            // Nothing to walk, or nothing to remove.
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    /**
     * Builds a test project the way the create action does: the mapper writes
     * the directories and their markers, then the indexer is told.
     */
    private TestProjectDirectoryDto create(final Path path) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto tp =
                    Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(getProject(), path);

            indexer().addTestProject(tp);
            return tp;
        });
    }

    /**
     * A node the plugin created is on disk, and the cache knows it.
     * <p>
     * Both halves, because either alone is the failure rule 2 is about: a cache
     * entry with no directory is the phantom it forbids, and a directory the
     * cache has not heard of is invisible to every surface.
     */
    public void testACreatedNodeIsOnDiskAndInTheCache() {
        final Path testProject = root.resolve("NAFATH");

        create(testProject);

        assertTrue("the test project is in the cache and not on disk - the phantom rule 2 forbids",
                Files.isDirectory(testProject));
        assertTrue("the cache has not heard of a node that was just created",
                indexer().nodeExists(testProject));
    }

    /**
     * The two fixed containers arrive with it.
     * <p>
     * A test project holds its Test Cases and Test Runs directories and nothing
     * else, so one without them is a project nothing can be created under - and
     * the folder names are the ones on disk, never a translated caption.
     */
    public void testATestProjectArrivesWithItsTwoContainers() {
        final Path testProject = root.resolve("NAFATH");

        create(testProject);

        assertTrue("the test cases directory is missing",
                Files.isDirectory(testProject.resolve(DirectoryType.TCD.getFolderName())));
        assertTrue("the test runs directory is missing",
                Files.isDirectory(testProject.resolve(DirectoryType.TRD.getFolderName())));
    }

    /**
     * And a node that was never made is not claimed.
     * <p>
     * The other half of the same question, and what a cache answers wrongly when
     * it is written before the file system rather than after.
     */
    public void testAnAbsentNodeIsNotInTheCache() {
        assertFalse("the cache claims a node that was never created",
                indexer().nodeExists(root.resolve("never-made")));
    }
}
