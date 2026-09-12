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

package org.testin.indexer;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.indexer.DirectoryMapper;
import org.testin.model.DirectoryType;
import org.testin.model.ProjectStatus;
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
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-100.
     * <p>
     * A test project that is not active is a node, and holds nothing.
     * <p>
     * It used to be skipped whole: not in the index at all, so the tree could
     * not draw it, the binding did not resolve, and the tester was sent to the
     * welcome screen to read in a sentence what the tree exists to say. It is
     * indexed now - drawn with "Inactive" beside its name, like any other
     * status - and its contents are not read, because a project nobody is
     * working on is not worth a directory walk.
     */
    public void testAnInactiveProjectIsANodeWithNothingInIt() {
        final Path testProject = root.resolve("NAFATH");

        final TestProjectDirectoryDto tp = create(testProject);

        WriteAction.runAndWait(() -> {
            tp.getMarker().setStatus(ProjectStatus.INACTIVE);
            indexer().persistMarker(tp);
        });

        indexer().scanSingleProject(testProject);

        assertTrue("an inactive project is still a node, so the tree can say what it is",
                indexer().nodeExists(testProject));

        assertTrue("nothing under an inactive project is read - it is not being worked on",
                indexer().getChildren(testProject).isEmpty());

        assertEquals("and it says which status it is",
                ProjectStatus.INACTIVE,
                indexer().find(testProject).orElseThrow().getMarker().status());
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
