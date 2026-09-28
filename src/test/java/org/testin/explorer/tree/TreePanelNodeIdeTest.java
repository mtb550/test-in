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

package org.testin.explorer.tree;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.tree.LeafState;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;

import java.nio.file.Path;

public class TreePanelNodeIdeTest extends BasePlatformTestCase {

    private static final Path SET = Path.of("project", "Test Cases", "Login");

    private static DirectoryDto at(final DirectoryDto directory, final Path path) {
        directory.setPath(path);
        return directory;
    }

    public void testARescanReadsTheSameFolderIntoAnEqualNode() {
        final TreePanelNode before = new TreePanelNode(getProject(), at(new TestSetDirectoryDto(), SET));
        final TreePanelNode after = new TreePanelNode(getProject(), at(new TestSetDirectoryDto(), SET));

        assertEquals("a rescan builds a new DTO for the same folder, and the tree must see the same node", before, after);
        assertEquals(before.hashCode(), after.hashCode());
    }

    public void testTwoFoldersAreTwoNodes() {
        final TreePanelNode login = new TreePanelNode(getProject(), at(new TestSetDirectoryDto(), SET));
        final TreePanelNode checkout = new TreePanelNode(getProject(), at(new TestSetDirectoryDto(), SET.resolveSibling("Checkout")));

        assertFalse(login.equals(checkout));
    }

    public void testATestSetAndATestRunHaveNothingToOpen() {
        assertEquals(LeafState.ALWAYS, new TreePanelNode(getProject(), at(new TestSetDirectoryDto(), SET)).getLeafState());
        assertEquals(LeafState.ALWAYS, new TreePanelNode(getProject(), at(new TestRunDirectoryDto(), Path.of("project", "Test Runs", "Cycle 1"))).getLeafState());
    }

    public void testAPackageIsAskedForItsChildren() {
        assertEquals(LeafState.ASYNC, new TreePanelNode(getProject(), at(new TestSetPackageDirectoryDto(), SET.getParent())).getLeafState());
    }
}
