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
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.TempTree;
import org.testin.model.DirectoryType;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WatchedProjectsIdeTest extends BasePlatformTestCase {

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-watched");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    public void testAChangeInAFolderWithoutAMarkerAddsNothing() {
        final Path notes = root.resolve("notes");
        try {
            Files.createDirectories(notes);
            Files.writeString(notes.resolve("todo.txt"), "not test data");
        } catch (final IOException ex) {
            throw new AssertionError("could not make the folder", ex);
        }

        indexer().rescanChangedProject(notes, new EmptyProgressIndicator());

        assertFalse("a folder with no .tp marker became a test project", indexer().nodeExists(notes));
    }

    public void testATestProjectIsReadAgainAndForgottenOnceItsMarkerIsGone() {
        final Path project = root.resolve("NAFATH");
        WriteAction.runAndWait(() -> indexer().addTestProject(
                Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(getProject(), project)));

        indexer().rescanChangedProject(project, new EmptyProgressIndicator());
        assertTrue("a test project with its marker was not read again", indexer().nodeExists(project));

        try {
            Files.delete(project.resolve(DirectoryType.TP.getMarker()));
        } catch (final IOException ex) {
            throw new AssertionError("could not remove the marker", ex);
        }

        indexer().rescanChangedProject(project, new EmptyProgressIndicator());
        assertFalse("a folder that is no longer a test project stayed in the index", indexer().nodeExists(project));
    }
}
