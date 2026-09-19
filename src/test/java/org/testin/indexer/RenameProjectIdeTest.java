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
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UC-TREE-PANEL-011, Rule-TREE-PANEL-004, Rule-TREE-PANEL-100.
 * <p>
 * Renaming a test project moves everything the index holds under it (#331).
 * <p>
 * A project is the one node whose parent is not indexed, whose containers are in
 * no map while it is inactive, and whose siblings are other projects the index
 * never read - so each of those is asked here, against a real folder.
 */
public class RenameProjectIdeTest extends BasePlatformTestCase {

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-rename");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

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

    private TestProjectDirectoryDto testProject(final String name) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto tp = Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(getProject(), root.resolve(name));
            indexer().addTestProject(tp);
            return tp;
        });
    }

    private TestSetDirectoryDto testSet(final TestProjectDirectoryDto tp) {
        return WriteAction.computeAndWait(() -> {
            final TestSetDirectoryDto ts = Services.getInstance(getProject(), DirectoryMapper.class)
                    .getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            indexer().addTestSet(ts);
            return ts;
        });
    }

    /**
     * Renames through the indexer and waits for it: the VFS work hops to a pooled
     * thread and back to the EDT, which this test runs on.
     */
    private void rename(final Path from, final Path to) {
        final AtomicBoolean done = new AtomicBoolean();
        indexer().renameNode(from, to, () -> done.set(true));
        PlatformTestUtil.waitWithEventsDispatching("the rename never finished", done::get, 15);
    }

    public void testARenamedProjectIsFoundUnderItsNewName() {
        final Path from = testProject("NAFATH").getPath();
        indexer().scanSingleProject(from);

        final Path to = root.resolve("Nafath_App");
        rename(from, to);

        assertTrue("the folder did not move", Files.isDirectory(to));
        assertTrue("the index does not hold the project under its new name", indexer().nodeExists(to));
        assertFalse("the index still holds the old name", indexer().nodeExists(from));
        assertTrue("its test cases folder did not follow", indexer().nodeExists(to.resolve("Test Cases")));
    }

    /**
     * An inactive project's containers are in none of the index's maps, so they
     * used to keep the old path, and a node created after reactivating it was
     * written into a folder that no longer existed.
     */
    public void testAnInactiveProjectsContainersFollowIt() {
        final TestProjectDirectoryDto tp = testProject("Checkout");
        WriteAction.runAndWait(() -> {
            tp.getMarker().setStatus(ProjectStatus.INACTIVE);
            indexer().persistMarker(tp);
        });
        indexer().scanSingleProject(tp.getPath());

        final Path to = root.resolve("Checkout_App");
        rename(tp.getPath(), to);

        final TestProjectDirectoryDto renamed = Optional.ofNullable(indexer().getTestProjectsByPath().get(to.toString()))
                .orElseThrow(() -> new AssertionError("the inactive project is not held under its new name"));
        assertEquals("its test cases folder kept the old path", to.resolve("Test Cases"), renamed.getTestCasesDirectory().getPath());
        assertEquals("its test runs folder kept the old path", to.resolve("Test Runs"), renamed.getTestRunsDirectory().getPath());
    }

    /**
     * Rule-INTERNAL-084. A case in a file named by hand is still found in that
     * file after the project around it is renamed - the store kept the old path,
     * so saving it could not take the hand-named file away.
     */
    public void testAHandNamedCaseKeepsItsFile() {
        final TestProjectDirectoryDto tp = testProject("NAFATH");
        final TestSetDirectoryDto ts = testSet(tp);
        final UUID id = UUID.randomUUID();

        try {
            Files.writeString(ts.getPath().resolve("login.json"), Services.getInstance(getProject(), Mapper.class)
                    .writeValueAsString(TestCaseDto.builder().id(id).description("Log in with a valid user").build()));
        } catch (final Exception ex) {
            throw new AssertionError("Could not write the hand-named case: " + ex.getMessage(), ex);
        }
        indexer().scanSingleProject(tp.getPath());

        final Path to = root.resolve("Nafath_App");
        rename(tp.getPath(), to);

        final TestCaseDto tc = indexer().findTestCase(id).orElseThrow();
        final TestCaseFile file = indexer().testCaseFile(tc).orElseThrow();

        assertEquals("the case is placed in the renamed project", to, file.testProject());
        assertEquals("the case lost its hand-named file", "login.json", file.inProject().getFileName().toString());
    }

    /**
     * Rule-TREE-PANEL-004. A name is taken when anything on disk has it - a
     * sibling project the index never read - and a rename that only changes case
     * is not in its own way.
     */
    public void testANameIsTakenOnDiskButNotByTheNodeItself() {
        final Path other = root.resolve("Payments");
        final Path self = root.resolve("nafath");
        try {
            Files.createDirectories(other);
            Files.createDirectories(self);
        } catch (final Exception ex) {
            throw new AssertionError("Could not make the folders: " + ex.getMessage(), ex);
        }

        assertTrue("a sibling folder the index never read was not seen", indexer().isTaken(other, Optional.empty()));
        assertTrue("a sibling is in the way of a rename", indexer().isTaken(other, Optional.of(self)));
        assertFalse("a free name was reported taken", indexer().isTaken(root.resolve("Checkout"), Optional.empty()));
        assertFalse("changing only the case of a name was refused as taken", indexer().isTaken(root.resolve("Nafath"), Optional.of(self)));
    }
}
