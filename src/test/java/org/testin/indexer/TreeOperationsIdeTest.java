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
import org.testin.model.DirectoryType;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.markers.TestCasesMainDirectoryMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class TreeOperationsIdeTest extends BasePlatformTestCase {

    private Path root;

    private static void deleteTree(final Path path) {
        if (path == null) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> {
                try {
                    Files.deleteIfExists(each);
                } catch (final Exception ignored) {
                }
            });
        } catch (final Exception ignored) {
        }
    }

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

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private TestProjectDirectoryDto create(final Path path) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto tp =
                    Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(getProject(), path);

            indexer().addTestProject(tp);
            return tp;
        });
    }

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

    public void testAFoldersIdIsWrittenOnceAndKept() {
        final Path testProject = root.resolve("NAFATH");

        final TestProjectDirectoryDto tp = create(testProject);
        final String stamped = tp.getMarker().getId();

        assertFalse("a folder Testin wrote carries an id", stamped.isEmpty());
        assertFalse("and so do the two containers under it",
                indexer().readMarker(testProject.resolve(DirectoryType.TCD.getFolderName()), DirectoryType.TCD, "Test Cases", TestCasesMainDirectoryMarker.class).getId().isEmpty());

        WriteAction.runAndWait(() -> {
            tp.getMarker().setStatus(ProjectStatus.INACTIVE);
            indexer().persistMarker(tp);
        });

        assertEquals("the id a folder has is the id it keeps",
                stamped,
                indexer().readMarker(testProject, DirectoryType.TP, "NAFATH", TestProjectMarker.class).getId());
    }

    public void testACreatedNodeIsOnDiskAndInTheCache() {
        final Path testProject = root.resolve("NAFATH");

        create(testProject);

        assertTrue("the test project is in the cache and not on disk - the phantom rule 2 forbids",
                Files.isDirectory(testProject));
        assertTrue("the cache has not heard of a node that was just created",
                indexer().nodeExists(testProject));
    }

    public void testATestProjectArrivesWithItsTwoContainers() {
        final Path testProject = root.resolve("NAFATH");

        create(testProject);

        assertTrue("the test cases directory is missing",
                Files.isDirectory(testProject.resolve(DirectoryType.TCD.getFolderName())));
        assertTrue("the test runs directory is missing",
                Files.isDirectory(testProject.resolve(DirectoryType.TRD.getFolderName())));
    }

    public void testAnAbsentNodeIsNotInTheCache() {
        assertFalse("the cache claims a node that was never created",
                indexer().nodeExists(root.resolve("never-made")));
    }
}
