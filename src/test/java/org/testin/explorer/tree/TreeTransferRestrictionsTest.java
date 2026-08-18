package org.testin.explorer.tree;

import org.testin.model.dto.dirs.*;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The tree operation restrictions: which nodes can be cut/copied/dragged,
 * renamed and removed, which targets accept which sources, and which
 * destinations are physically valid.
 */
public class TreeTransferRestrictionsTest {

    private static DirectoryDto project(final String root, final String name) {
        final TestProjectDirectoryDto dto = new TestProjectDirectoryDto();
        dto.setPath(Path.of(root, name));
        return dto;
    }

    private static DirectoryDto childPackage(final DirectoryDto parent, final String name) {
        final TestSetPackageDirectoryDto dto = new TestSetPackageDirectoryDto();
        dto.setPath(parent.getPath().resolve(name));
        dto.setParent(parent);
        return dto;
    }

    private static DirectoryDto node(final String first, final String... more) {
        final TestSetPackageDirectoryDto dto = new TestSetPackageDirectoryDto();
        dto.setPath(Path.of(first, more));
        return dto;
    }

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

    /**
     * A test project is fixed in the tree in every way but one: it cannot be
     * moved and cannot be renamed - its name is the directory every path under
     * it is built from - and it can be removed.
     * <p>
     * Removing it was switched off with the rest of the restrictions in
     * 5f6e0a87 and turned back on deliberately: the tester who made a project
     * is the one who deletes it, behind a confirmation that counts the test
     * sets, cases and runs going with it.
     */
    @Test
    public void aTestProjectIsRemovableButNeverMovedOrRenamed() {
        final DirectoryDto testProject = new TestProjectDirectoryDto();

        assertTrue(testProject.isRemovable(), "a test project is removed by the tester who made it");
        assertFalse(testProject.isTransferable(), "a test project is not cut, copied or dragged");
        assertFalse(testProject.isRenamable(), "every path under it is built from its name");
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

    @Test
    public void runNodesNeverEnterTheTestSetFamily() {
        final DirectoryDto[] testSetFamilyTargets = {
                new TestCasesMainDirectoryDto(),
                new TestSetPackageDirectoryDto()
        };
        final DirectoryDto[] runSources = {new TestRunDirectoryDto(), new TestRunPackageDirectoryDto()};

        for (final DirectoryDto target : testSetFamilyTargets) {
            for (final DirectoryDto source : runSources) {
                assertFalse(target.acceptsTransferred(source),
                        target.getClass().getSimpleName() + " must reject " + source.getClass().getSimpleName());
            }
            assertTrue(target.acceptsTransferred(new TestSetDirectoryDto()),
                    target.getClass().getSimpleName() + " must accept test-set nodes");
        }
    }

    @Test
    public void testSetAcceptsNoDirectoryNodes() {
        final DirectoryDto testSet = new TestSetDirectoryDto();

        assertFalse(testSet.isTransferTarget(), "a test set holds test cases only");
        assertFalse(testSet.acceptsTransferred(new TestSetPackageDirectoryDto()), "no package into a test set");
        assertFalse(testSet.acceptsTransferred(new TestSetDirectoryDto()), "no test set into a test set");
        assertFalse(testSet.acceptsTransferred(new TestRunDirectoryDto()), "no run node into a test set");
    }

    @Test
    public void testSetNodesNeverEnterTheRunFamily() {
        final DirectoryDto[] runFamilyTargets = {
                new TestRunsMainDirectoryDto(),
                new TestRunPackageDirectoryDto(),
                new TestRunDirectoryDto()
        };
        final DirectoryDto[] testSetSources = {new TestSetDirectoryDto(), new TestSetPackageDirectoryDto()};

        for (final DirectoryDto target : runFamilyTargets) {
            for (final DirectoryDto source : testSetSources) {
                assertFalse(target.acceptsTransferred(source),
                        target.getClass().getSimpleName() + " must reject " + source.getClass().getSimpleName());
            }
        }
        assertTrue(new TestRunsMainDirectoryDto().acceptsTransferred(new TestRunPackageDirectoryDto()),
                "the runs root must accept run packages");
        assertTrue(new TestRunPackageDirectoryDto().acceptsTransferred(new TestRunPackageDirectoryDto()),
                "run packages must accept run packages");
    }

    @Test
    public void testRunAcceptsNoRunStructure() {
        final DirectoryDto testRun = new TestRunDirectoryDto();

        assertFalse(testRun.acceptsTransferred(new TestRunDirectoryDto()),
                "no test run into a test run");
        assertFalse(testRun.acceptsTransferred(new TestRunPackageDirectoryDto()),
                "no run package into a test run");

        assertTrue(new TestRunsMainDirectoryDto().acceptsTransferred(new TestRunDirectoryDto()),
                "the runs root must still accept test runs");
        assertTrue(new TestRunPackageDirectoryDto().acceptsTransferred(new TestRunDirectoryDto()),
                "run packages must still accept test runs");
    }

    @Test
    public void testProjectAcceptsNothing() {
        final DirectoryDto testProject = new TestProjectDirectoryDto();

        assertFalse(testProject.isTransferTarget());
        assertFalse(testProject.acceptsTransferred(new TestSetDirectoryDto()));
        assertFalse(testProject.acceptsTransferred(new TestRunDirectoryDto()));
    }

    @Test
    public void destinationMustNotBeSelfSubtreeOrParent() {
        final DirectoryDto source = node("root", "test-cases", "pkg");

        assertFalse(TreeTransferHandler.isValidDestination(source, node("root", "test-cases", "pkg"), path -> false),
                "onto itself must be invalid");
        assertFalse(TreeTransferHandler.isValidDestination(source, node("root", "test-cases", "pkg", "inner"), path -> false),
                "into its own subtree must be invalid");
        assertFalse(TreeTransferHandler.isValidDestination(source, node("root", "test-cases"), path -> false),
                "into its own parent must be invalid - this was the IO-exception copy");

        assertTrue(TreeTransferHandler.isValidDestination(source, node("root", "test-cases", "other"), path -> false),
                "an unrelated sibling target must stay valid");
    }

    @Test
    public void destinationMustNotAlreadyContainTheName() {
        final DirectoryDto source = node("root", "test-cases", "pkg");
        final DirectoryDto target = node("root", "test-cases", "other");
        final Path occupiedPath = Path.of("root", "test-cases", "other", "pkg");

        assertFalse(TreeTransferHandler.isValidDestination(source, target, occupiedPath::equals),
                "a target already containing the name must be invalid - the 'already exists in VFS' case");
        assertTrue(TreeTransferHandler.isValidDestination(source, target, path -> false),
                "the same target is valid when the name is free");
    }

    @Test
    public void transfersNeverCrossTestProjects() {
        final DirectoryDto projectA = project("Testin", "projectA");
        final DirectoryDto packageInA = childPackage(projectA, "pkg");
        final DirectoryDto casesDirInA = childPackage(projectA, "Test Cases");

        final DirectoryDto projectB = project("Testin", "projectB");
        final DirectoryDto packageInB = childPackage(projectB, "pkg2");

        assertTrue(TreeTransferHandler.sameTestProject(packageInA, casesDirInA),
                "within one project must stay allowed");
        assertFalse(TreeTransferHandler.sameTestProject(packageInA, packageInB),
                "across projects must be rejected, whatever the node types");

        final DirectoryDto orphan = new TestSetPackageDirectoryDto();
        orphan.setPath(Path.of("somewhere", "pkg"));
        assertFalse(TreeTransferHandler.sameTestProject(orphan, packageInA),
                "unresolvable ownership must reject");
    }
}
