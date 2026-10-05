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

package org.testin.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.StandIn;
import org.testin.creator.NodeCreators;
import org.testin.model.node.DirectoryDto;
import org.testin.model.node.TestCasesMainDirectoryDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.node.TestRunPackageDirectoryDto;
import org.testin.model.node.TestRunsMainDirectoryDto;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.model.node.TestSetPackageDirectoryDto;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.ProjectStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.notifications.Refused;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TreeNodeKindsTest {

    private static @NotNull List<DirectoryDto> everyKind() {
        return List.of(new TestProjectDirectoryDto(), new TestCasesMainDirectoryDto(), new TestRunsMainDirectoryDto(),
                new TestSetPackageDirectoryDto(), new TestRunPackageDirectoryDto(), new TestSetDirectoryDto(), new TestRunDirectoryDto());
    }

    private static @NotNull TestRunDirectoryDto testRunIn(final @NotNull TestRunStatus status) {
        final @NotNull TestRunDirectoryDto testRun = new TestRunDirectoryDto();
        testRun.getMarker().changeStatus(status);
        return testRun;
    }

    private static @NotNull String name(final @NotNull DirectoryDto node) {
        return node.getClass().getSimpleName();
    }

    // Rule-TREE-PANEL-002
    @Test
    public void theTwoContainersAreNeverCreatedAndComeWithTheTestProject() {
        for (final DirectoryDto node : everyKind()) {
            assertFalse(node.childKinds().contains(DirectoryType.TCD), name(node) + " offers to create Test Cases");
            assertFalse(node.childKinds().contains(DirectoryType.TRD), name(node) + " offers to create Test Runs");
        }

        final @NotNull Project p = StandIn.of(Project.class);
        final @NotNull DirectoryDto parent = new TestProjectDirectoryDto();
        for (final DirectoryType container : List.of(DirectoryType.TCD, DirectoryType.TRD)) {
            assertTrue(NodeCreators.of(p, container).execute("Extra", parent, Path.of("root", "Extra")).isEmpty(),
                    container.getDescription() + " was created from the tree");
        }

        final @NotNull TestProjectDirectoryDto testProject = new TestProjectDirectoryDto();
        assertEquals(testProject.fixedChildren().stream().map(DirectoryDto::getType).toList(), List.of(DirectoryType.TCD, DirectoryType.TRD),
                "a test project does not arrive holding its two containers");
    }

    // Rule-TREE-PANEL-022
    @Test
    public void onlyATestSetAndATestRunOpenInAnEditor() {
        for (final DirectoryDto node : everyKind()) {
            final boolean opens = node instanceof TestSetDirectoryDto || node instanceof TestRunDirectoryDto;

            assertEquals(node.isOpenableInEditor(), opens, name(node) + (opens ? " does not open" : " has nothing to open, and opens"));
        }
    }

    // Rule-TREE-PANEL-024
    @Test
    public void underTestCasesOnlyATestSetOrATestSetPackageIsCreated() {
        assertEquals(new TestCasesMainDirectoryDto().childKinds(), List.of(DirectoryType.TS, DirectoryType.TSP));
        assertEquals(new TestSetPackageDirectoryDto().childKinds(), List.of(DirectoryType.TS, DirectoryType.TSP));
    }

    // Rule-TREE-PANEL-032
    @Test
    public void underTestRunsOnlyATestRunOrATestRunPackageIsCreated() {
        assertEquals(new TestRunsMainDirectoryDto().childKinds(), List.of(DirectoryType.TR, DirectoryType.TRP));
        assertEquals(new TestRunPackageDirectoryDto().childKinds(), List.of(DirectoryType.TR, DirectoryType.TRP));
    }

    // Rule-TREE-PANEL-025
    @Test
    public void nothingIsCreatedUnderATestProjectATestSetOrATestRun() {
        for (final DirectoryDto node : List.of(new TestProjectDirectoryDto(), new TestSetDirectoryDto(), new TestRunDirectoryDto())) {
            assertFalse(node.canCreateChildren(), name(node) + " offers to create a node under it");
            assertTrue(node.childKinds().isEmpty(), name(node) + " names kinds it would create");
        }
    }

    // Rule-TREE-PANEL-028
    @Test
    public void aTestSetPackageHoldsAnotherTestSetPackage() {
        assertTrue(new TestSetPackageDirectoryDto().childKinds().contains(DirectoryType.TSP));
        assertTrue(new TestSetPackageDirectoryDto().acceptsTransferred(new TestSetPackageDirectoryDto()));
    }

    // Rule-TREE-PANEL-034
    @Test
    public void aTestRunPackageHoldsAnotherTestRunPackage() {
        assertTrue(new TestRunPackageDirectoryDto().childKinds().contains(DirectoryType.TRP));
        assertTrue(new TestRunPackageDirectoryDto().acceptsTransferred(new TestRunPackageDirectoryDto()));
    }

    // Rule-TREE-PANEL-080
    @Test
    public void runTestsIsOfferedOnTheTestCaseSideAndOnATestRun() {
        for (final DirectoryDto node : everyKind()) {
            final boolean runsTestCases = node instanceof TestCasesMainDirectoryDto || node instanceof TestSetPackageDirectoryDto || node instanceof TestSetDirectoryDto;

            assertEquals(node.isTestCaseContainer(), runsTestCases, name(node) + (runsTestCases ? " is not offered Run Tests" : " is offered Run Tests"));
        }

        assertTrue(new TestRunDirectoryDto().isStillOpen(), "a new test run is not offered Run Tests");
    }

    // Rule-TREE-PANEL-094
    @Test
    public void aSignedOffTestRunIsRemovedButNotRenamedNumberedOrDragged() {
        for (final TestRunStatus status : TestRunStatus.values()) {
            final @NotNull TestRunDirectoryDto testRun = testRunIn(status);
            final boolean signedOff = status == TestRunStatus.COMPLETED || status == TestRunStatus.CLOSED;

            assertTrue(testRun.isRemovable(), "a " + status + " test run cannot be removed");
            assertEquals(testRun.isRenamable(), !signedOff, "a " + status + " test run");
            assertEquals(testRun.isOrderable(), !signedOff, "a " + status + " test run");
            assertEquals(testRun.isTransferable(), !signedOff, "a " + status + " test run");
        }
    }

    // Rule-TREE-PANEL-099
    @Test
    public void aNodeSaysItsStatusOnlyWhenItIsNotActive() {
        final @NotNull TestProjectDirectoryDto testProject = new TestProjectDirectoryDto();
        assertEquals(testProject.statusShownInTree(), "", "an active test project says Active");
        testProject.getMarker().setStatus(ProjectStatus.INACTIVE);
        assertEquals(testProject.statusShownInTree(), ProjectStatus.INACTIVE.getLabel());

        final @NotNull TestSetDirectoryDto testSet = new TestSetDirectoryDto();
        assertEquals(testSet.statusShownInTree(), "", "an active test set says Active");
        testSet.getMarker().setStatus(TestSetStatus.DEPRECATED);
        assertEquals(testSet.statusShownInTree(), TestSetStatus.DEPRECATED.getLabel());

        final @NotNull TestSetPackageDirectoryDto testSetPackage = new TestSetPackageDirectoryDto();
        assertEquals(testSetPackage.statusShownInTree(), "", "an active package says Active");
        testSetPackage.getMarker().setStatus(PackageStatus.ARCHIVED);
        assertEquals(testSetPackage.statusShownInTree(), PackageStatus.ARCHIVED.getLabel());

        assertEquals(new TestCasesMainDirectoryDto().statusShownInTree(), "", "a container has no status to say");

        for (final TestRunStatus status : TestRunStatus.values()) {
            assertEquals(testRunIn(status).statusShownInTree(), status.getLabel(), "a test run does not always say its status");
        }
    }

    // Rule-TREE-PANEL-005
    @Test
    public void aNodeNameIsNeverEmpty() {
        for (final DirectoryType kind : DirectoryType.values()) {
            for (final String empty : List.of("", " ", "\t")) {
                assertEquals(Refused.ofName(kind, empty), Optional.of(Refused.NOT_ONE_FOLDER), kind + " takes the name '" + empty + "'");
                assertFalse(kind.canTakeName(empty), kind + " takes the name '" + empty + "'");
            }
        }
    }

    // Rule-TREE-PANEL-095
    @Test
    public void aNodeNameIsOneFolderAndAJavaPackageNameWhereItBecomesOne() {
        for (final DirectoryType kind : DirectoryType.values()) {
            for (final String path : List.of("Login/Admin", "Login\\Admin", "..", ".")) {
                assertEquals(Refused.ofName(kind, path), Optional.of(Refused.NOT_ONE_FOLDER), kind + " takes the path '" + path + "' as a name");
            }

            assertEquals(Refused.ofName(kind, "Login"), Optional.empty(), kind + " refuses an ordinary name");
        }

        for (final DirectoryType becomesAJavaPackage : List.of(DirectoryType.TP, DirectoryType.TSP)) {
            for (final String javaWord : List.of("new", "class", "import")) {
                assertEquals(Refused.ofName(becomesAJavaPackage, javaWord), Optional.of(Refused.NOT_A_JAVA_NAME),
                        becomesAJavaPackage + " takes '" + javaWord + "', which Java refuses as a package");
            }
        }
    }
}
