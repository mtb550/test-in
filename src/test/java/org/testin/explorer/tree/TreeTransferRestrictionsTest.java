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

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.DirectoryDto;
import org.testin.model.node.TestCasesMainDirectoryDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.node.TestRunPackageDirectoryDto;
import org.testin.model.node.TestRunsMainDirectoryDto;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.model.node.TestSetPackageDirectoryDto;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TreeTransferRestrictionsTest {

    private static @NotNull DirectoryDto project(final String name) {
        final TestProjectDirectoryDto dto = new TestProjectDirectoryDto();
        dto.setPath(Path.of("Testin", name));
        return dto;
    }

    private static @NotNull DirectoryDto childPackage(final DirectoryDto parent, final String name) {
        final TestSetPackageDirectoryDto dto = new TestSetPackageDirectoryDto();
        dto.setPath(parent.getPath().resolve(name));
        dto.setParent(parent);
        return dto;
    }

    private static @NotNull DirectoryDto node(final String... underRoot) {
        final TestSetPackageDirectoryDto dto = new TestSetPackageDirectoryDto();
        dto.setPath(Path.of("root", underRoot));
        return dto;
    }

    // Rule-PRODUCT-006, Rule-TREE-PANEL-002
    @Test
    public void fixedNodesCannotBeMovedRenamedOrRemoved() {
        final DirectoryDto[] fixed = {
                new TestCasesMainDirectoryDto(),
                new TestRunsMainDirectoryDto()
        };

        for (final DirectoryDto node : fixed) {
            assertFalse(node.isTransferable(), node.getClass().getSimpleName() + " must not be cut/copied/dragged");
            assertFalse(node.isRemovable(), node.getClass().getSimpleName() + " must not be removable");
            assertFalse(node.isRenamable(), node.getClass().getSimpleName() + " must not be renamable");
        }
    }

    @Test
    public void aTestProjectIsRemovableAndRenamableButNeverMoved() {
        final DirectoryDto testProject = new TestProjectDirectoryDto();

        assertTrue(testProject.isRemovable(), "a test project is removed by the tester who made it");
        assertFalse(testProject.isTransferable(), "a test project is not cut, copied or dragged");
        assertTrue(testProject.isRenamable(), "a clone takes its repository's name, and the tester can change it");
    }

    @Test
    public void regularNodesKeepTheirCapabilities() {
        final DirectoryDto[] regular = {
                new TestSetDirectoryDto(),
                new TestSetPackageDirectoryDto(),
                new TestRunDirectoryDto(),
                new TestRunPackageDirectoryDto()
        };

        for (final DirectoryDto node : regular) {
            assertTrue(node.isTransferable(), node.getClass().getSimpleName() + " must stay transferable");
            assertTrue(node.isRemovable(), node.getClass().getSimpleName() + " must stay removable");
            assertTrue(node.isRenamable(), node.getClass().getSimpleName() + " must stay renamable");
        }
    }

    // Rule-PRODUCT-005, Rule-TREE-PANEL-003, Rule-TREE-PANEL-043
    @Test
    public void testRunNodesNeverEnterTheTestSetFamily() {
        final DirectoryDto[] testSetFamilyTargets = {
                new TestCasesMainDirectoryDto(),
                new TestSetPackageDirectoryDto()
        };
        final DirectoryDto[] testRunSources = {new TestRunDirectoryDto(), new TestRunPackageDirectoryDto()};

        for (final DirectoryDto target : testSetFamilyTargets) {
            for (final DirectoryDto source : testRunSources) {
                assertFalse(target.acceptsTransferred(source),
                        target.getClass().getSimpleName() + " must reject " + source.getClass().getSimpleName());
            }
            assertTrue(target.acceptsTransferred(new TestSetDirectoryDto()),
                    target.getClass().getSimpleName() + " must accept test-set nodes");
        }
    }

    // Rule-TREE-PANEL-044
    @Test
    public void testSetAcceptsNoDirectoryNodes() {
        final DirectoryDto testSet = new TestSetDirectoryDto();

        assertFalse(testSet.isTransferTarget(), "a test set holds test cases only");
        assertFalse(testSet.acceptsTransferred(new TestSetPackageDirectoryDto()), "no package into a test set");
        assertFalse(testSet.acceptsTransferred(new TestSetDirectoryDto()), "no test set into a test set");
        assertFalse(testSet.acceptsTransferred(new TestRunDirectoryDto()), "no test run node into a test set");
    }

    // Rule-PRODUCT-005, Rule-TREE-PANEL-003, Rule-TREE-PANEL-043
    @Test
    public void testSetNodesNeverEnterTheTestRunFamily() {
        final DirectoryDto[] testRunFamilyTargets = {
                new TestRunsMainDirectoryDto(),
                new TestRunPackageDirectoryDto(),
                new TestRunDirectoryDto()
        };
        final DirectoryDto[] testSetSources = {new TestSetDirectoryDto(), new TestSetPackageDirectoryDto()};

        for (final DirectoryDto target : testRunFamilyTargets) {
            for (final DirectoryDto source : testSetSources) {
                assertFalse(target.acceptsTransferred(source),
                        target.getClass().getSimpleName() + " must reject " + source.getClass().getSimpleName());
            }
        }
        assertTrue(new TestRunsMainDirectoryDto().acceptsTransferred(new TestRunPackageDirectoryDto()),
                "the test runs root must accept test run packages");
        assertTrue(new TestRunPackageDirectoryDto().acceptsTransferred(new TestRunPackageDirectoryDto()),
                "test run packages must accept test run packages");
    }

    // Rule-PRODUCT-005, Rule-TREE-PANEL-044
    @Test
    public void testRunAcceptsNoRunStructure() {
        final DirectoryDto testRun = new TestRunDirectoryDto();

        assertFalse(testRun.acceptsTransferred(new TestRunDirectoryDto()),
                "no test run into a test run");
        assertFalse(testRun.acceptsTransferred(new TestRunPackageDirectoryDto()),
                "no test run package into a test run");

        assertTrue(new TestRunsMainDirectoryDto().acceptsTransferred(new TestRunDirectoryDto()),
                "the test runs root must still accept test runs");
        assertTrue(new TestRunPackageDirectoryDto().acceptsTransferred(new TestRunDirectoryDto()),
                "test run packages must still accept test runs");
    }

    @Test
    public void testProjectAcceptsNothing() {
        final DirectoryDto testProject = new TestProjectDirectoryDto();

        assertFalse(testProject.isTransferTarget());
        assertFalse(testProject.acceptsTransferred(new TestSetDirectoryDto()));
        assertFalse(testProject.acceptsTransferred(new TestRunDirectoryDto()));
    }

    // Rule-TREE-PANEL-045
    @Test
    public void destinationMustNotBeSelfSubtreeOrParent() {
        final DirectoryDto source = node("test-cases", "pkg");

        assertFalse(TreeTransferHandler.isValidDestination(source, node("test-cases", "pkg"), _ -> false),
                "onto itself must be invalid");
        assertFalse(TreeTransferHandler.isValidDestination(source, node("test-cases", "pkg", "inner"), _ -> false),
                "into its own subtree must be invalid");
        assertFalse(TreeTransferHandler.isValidDestination(source, node("test-cases"), _ -> false),
                "into its own parent must be invalid - this was the IO-exception copy");

        assertTrue(TreeTransferHandler.isValidDestination(source, node("test-cases", "other"), _ -> false),
                "an unrelated sibling target must stay valid");
    }

    // Rule-TREE-PANEL-004
    @Test
    public void destinationMustNotAlreadyContainTheName() {
        final DirectoryDto source = node("test-cases", "pkg");
        final DirectoryDto target = node("test-cases", "other");
        final Path occupiedPath = Path.of("root", "test-cases", "other", "pkg");

        assertFalse(TreeTransferHandler.isValidDestination(source, target, occupiedPath::equals),
                "a target already containing the name must be invalid - the 'already exists in VFS' case");
        assertTrue(TreeTransferHandler.isValidDestination(source, target, _ -> false),
                "the same target is valid when the name is free");
    }

    // Rule-TREE-PANEL-013
    @Test
    public void transfersNeverCrossTestProjects() {
        final DirectoryDto projectA = project("projectA");
        final DirectoryDto packageInA = childPackage(projectA, "pkg");
        final DirectoryDto testCasesDirInA = childPackage(projectA, "Test Cases");

        final DirectoryDto projectB = project("projectB");
        final DirectoryDto packageInB = childPackage(projectB, "pkg2");

        assertTrue(TreeTransferHandler.sameTestProject(packageInA, testCasesDirInA),
                "within one project must stay allowed");
        assertFalse(TreeTransferHandler.sameTestProject(packageInA, packageInB),
                "across projects must be rejected, whatever the node types");

        final DirectoryDto orphan = new TestSetPackageDirectoryDto();
        orphan.setPath(Path.of("somewhere", "pkg"));
        assertFalse(TreeTransferHandler.sameTestProject(orphan, packageInA),
                "unresolvable ownership must reject");
    }
}
