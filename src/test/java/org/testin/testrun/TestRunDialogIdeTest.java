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

package org.testin.testrun;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.creator.CreateTestRun;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestRuns;
import org.testin.model.PackageStatus;
import org.testin.model.RunItemStatus;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.TestSetStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.OnScreenDialog;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TestRunDialogIdeTest extends AbstractOpenEditorsIdeTest {

    private TestProjectDirectoryDto tp;
    private TestSetDirectoryDto login;
    private TestCaseDto first;
    private TestCaseDto second;
    private TestSetDirectoryDto empty;
    private TestSetDirectoryDto old;
    private TestCaseDto inOld;
    private TestSetPackageDirectoryDto archive;
    private TestSetDirectoryDto inside;
    private TestCaseDto inInside;

    @Override
    public void setUp() {
        super.setUp();
        Services.getInstance(getProject(), ProjectIndexer.class).resetForReindex();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        tp = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(tp.getTestCasesDirectory(), "Login");
        first = made.testCase(login);
        second = made.testCase(login);
        empty = made.testSet(tp.getTestCasesDirectory(), "Empty");
        old = made.testSet(tp.getTestCasesDirectory(), "Old");
        inOld = made.testCase(old);
        archive = made.testSetPackage(tp.getTestCasesDirectory(), "Archive");
        inside = made.testSet(archive, "Inside");
        inInside = made.testCase(inside);
        assertTrue(nodes().mark(old, TestSetStatus.DEPRECATED, "Mohammed AlZamil"));
        assertTrue(nodes().mark(archive, PackageStatus.ARCHIVED, "Mohammed AlZamil"));
        bound().choose("NAFATH");
    }

    @Override
    public void tearDown() {
        OnScreenDialog.closed(getProject(), TestRunConfigurationDialog.class);
        bound().choose("");
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private static @NotNull Map<TestRunConfiguration, String> everyQuestionAnswered() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field.isChoice()) answers.put(field, field.getOptions()[0]);
        }
        return answers;
    }

    private @NotNull TestRunDirectoryDto cycle1In(final @NotNull DirectoryDto parent, final @NotNull List<TestRunItems> results) {
        final @NotNull TestRunDirectoryDto testRun = new NodesOnDisk(getProject()).testRun(parent, "Cycle-1");
        indexedTestRuns().putTestRun(testRun.getPath(), new TestRunDto().setResults(new ArrayList<>(results)));
        return testRun;
    }

    private void theCreateDialogOpened() {
        new CreateTestRun(getProject()).configureTestRun(tp.getTestCasesDirectory(), "Cycle-1", tp.getTestRunsDirectory(), Set.of(), everyQuestionAnswered());
        Await.until("the test run dialog never opened", () -> OnScreenDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        OnScreenDialog.sized(getProject(), TestRunConfigurationDialog.class);
    }

    private void theEditDialogOpenedOn(final @NotNull TestRunDirectoryDto testRun) {
        new EditTestRunWork(getProject()).editAt(new TreePath(new DefaultMutableTreeNode(testRun)));
        Await.until("the edit dialog never opened", () -> OnScreenDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        OnScreenDialog.sized(getProject(), TestRunConfigurationDialog.class);
    }

    private @NotNull JComponent content() {
        return OnScreenDialog.content(getProject(), TestRunConfigurationDialog.class);
    }

    private @NotNull CheckboxTree theTestCaseTree() {
        return Optional.ofNullable(UIUtil.uiTraverser(content()).filter(CheckboxTree.class).first()).orElseThrow(() -> new AssertionError("the dialog shows no test case tree"));
    }

    private @NotNull List<CheckedTreeNode> rows() {
        final @NotNull List<CheckedTreeNode> found = new ArrayList<>();
        final @NotNull CheckedTreeNode top = (CheckedTreeNode) theTestCaseTree().getModel().getRoot();
        for (final Object each : Collections.list(top.depthFirstEnumeration())) found.add((CheckedTreeNode) each);
        return found;
    }

    private @NotNull List<Object> offered() {
        return rows().stream().map(CheckedTreeNode::getUserObject).toList();
    }

    private @NotNull CheckedTreeNode rowOf(final @NotNull Object held) {
        return rows().stream().filter(row -> row.getUserObject().equals(held)).findFirst().orElseThrow(() -> new AssertionError(held + " is not offered: " + offered()));
    }

    private @NotNull JButton theButton(final @NotNull String text) {
        return Drawn.components(content()).stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no " + text + " button: " + Drawn.words(content())));
    }

    private @NotNull List<DirectoryDto> theTestRunsUnder(final @NotNull DirectoryDto parent) {
        return nodes().getChildren(parent.getPath());
    }

    private static boolean answers(final @NotNull Shortcut shortcut, final @NotNull KeyStroke key) {
        return shortcut instanceof final KeyboardShortcut keyboard && key.equals(keyboard.getFirstKeyStroke()) && keyboard.getSecondKeyStroke() == null;
    }

    private static @NotNull AnAction actionFor(final @NotNull JComponent component, final @NotNull KeyStroke key) {
        return ActionUtil.getActions(component).stream()
                .filter(action -> Arrays.stream(action.getShortcutSet().getShortcuts()).anyMatch(shortcut -> answers(shortcut, key)))
                .findFirst()
                .orElseThrow(() -> new AssertionError(component.getClass().getSimpleName() + " does not answer " + key));
    }

    // Rule-TREE-PANEL-030
    public void testARetiredOrEmptyTestSetIsNotOffered() {
        theCreateDialogOpened();
        final @NotNull List<Object> offered = offered();

        assertTrue("a test set that holds test cases is not offered: " + offered, offered.containsAll(List.of(login, first, second)));
        for (final Object left : List.of(empty, old, inOld, archive, inside, inInside)) {
            assertFalse(left + " is offered for a new test run: " + offered, offered.contains(left));
        }
    }

    // Rule-TREE-PANEL-029
    public void testATestRunCannotBeCreatedEmpty() {
        theCreateDialogOpened();
        final @NotNull CheckboxTree tree = theTestCaseTree();
        final @NotNull JButton create = theButton(Bundle.message("test.run.create.button"));

        tree.setNodeState((CheckedTreeNode) tree.getModel().getRoot(), false);

        assertFalse("Create works with no test case checked", create.isEnabled());
        assertEquals("hovering over the gray Create does not say why", Bundle.message("test.run.form.no.test.case"), Drawn.hovering(create));
        create.doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertTrue("an empty test run was created", theTestRunsUnder(tp.getTestRunsDirectory()).isEmpty());

        tree.setNodeState(rowOf(first), true);

        assertTrue("Create is gray with a test case checked", create.isEnabled());
    }

    // Rule-TREE-PANEL-122
    public void testEnterDoesNothingInTheChangeLogOrTheName() {
        theCreateDialogOpened();
        final @NotNull EditorTextField changeLog = Optional.ofNullable(UIUtil.uiTraverser(content()).filter(EditorTextField.class).first()).orElseThrow(() -> new AssertionError("the dialog has no change log"));
        final @NotNull JTextField name = Optional.ofNullable(UIUtil.uiTraverser(content()).filter(JTextField.class).filter(JTextField::isEditable).first()).orElseThrow(() -> new AssertionError("the dialog has no name field"));

        final @NotNull AnAction enter = actionFor(changeLog, Shortcuts.Enter.getKey());
        ActionUtil.performAction(enter, TestActionEvent.createTestEvent(enter));
        name.postActionEvent();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("Enter changed the change log", "", changeLog.getText());
        assertTrue("Enter closed the dialog", OnScreenDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        assertTrue("Enter created the test run", theTestRunsUnder(tp.getTestRunsDirectory()).isEmpty());
    }

    // Rule-TREE-PANEL-093
    public void testTheEditDialogTicksWhatTheTestRunCoversAndAFolderOnlyWhenAllUnderItIs() {
        final @NotNull TestSetDirectoryDto card = new NodesOnDisk(getProject()).testSet(tp.getTestCasesDirectory(), "Card");
        final @NotNull TestCaseDto paid = new NodesOnDisk(getProject()).testCase(card);
        final @NotNull TestRunDirectoryDto testRun = cycle1In(tp.getTestRunsDirectory(), List.of(new TestRunItems().setId(first.getId()), new TestRunItems().setId(paid.getId())));

        theEditDialogOpenedOn(testRun);

        assertTrue("a test case the test run covers is not ticked", rowOf(first).isChecked());
        assertFalse("a test case the test run does not cover is ticked", rowOf(second).isChecked());
        assertFalse("a folder is ticked over a test case the test run does not cover", rowOf(login).isChecked());
        assertTrue("a folder whose every test case is covered is not ticked", rowOf(card).isChecked());
        assertTrue("a covered test case under a fully covered folder is not ticked", rowOf(paid).isChecked());
    }

    // Rule-TREE-PANEL-076
    public void testSavingTheEditKeepsWhatTheDialogDoesNotShow() {
        final @NotNull UUID deleted = UUID.randomUUID();
        final @NotNull TestRunDirectoryDto testRun = cycle1In(tp.getTestRunsDirectory(), List.of(new TestRunItems().setId(first.getId()), new TestRunItems().setId(inOld.getId()).setStatus(RunItemStatus.FAILED), new TestRunItems().setId(deleted).setStatus(RunItemStatus.PASSED)));

        indexedTestRuns().changeTestRunMarker(testRun.getPath(), marker -> marker.configure(everyQuestionAnswered()));
        theEditDialogOpenedOn(testRun);
        final @NotNull List<Object> offered = offered();
        assertFalse("a test case of a deprecated test set is offered", offered.contains(inOld));
        assertTrue("a test case deleted since is offered", offered.stream().noneMatch(row -> row instanceof final TestCaseDto tc && tc.getId().equals(deleted)));

        theButton(StatusBarShortcut.SAVE).doClick();

        Await.until("saving the edit did not close the dialog", () -> !OnScreenDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        final @NotNull TestRunDto saved = indexedTestRuns().getTestRunByPath(testRun.getPath());
        assertEquals("saving removed what the test run recorded for a test case the dialog did not show", Set.of(first.getId(), inOld.getId(), deleted), saved.coveredIds());
        assertEquals(RunItemStatus.FAILED, saved.resultOf(inOld.getId()).map(TestRunItems::getStatus).orElseThrow());
        assertEquals(RunItemStatus.PASSED, saved.resultOf(deleted).map(TestRunItems::getStatus).orElseThrow());
    }

    // Rule-TREE-PANEL-070, Rule-TREE-PANEL-072
    public void testReCreatingCarriesOnlyTheTestCasesAndTheConfigurationIntoTheSameFolder() {
        final @NotNull TestRunPackageDirectoryDto sprint = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunPackageDirectoryDto made = Services.getInstance(getProject(), DirectoryMapper.class).getTestRunPackageNode(tp.getTestRunsDirectory().getPath().resolve("Sprint"), tp.getTestRunsDirectory());
            nodes().addTestRunPackage(made);
            return made;
        });
        final @NotNull TestRunDirectoryDto source = cycle1In(sprint, List.of(new TestRunItems().setId(first.getId()).setStatus(RunItemStatus.FAILED).setDuration(Duration.ofSeconds(5)).setActualResult("The dashboard never opened").setStacktrace("java.lang.AssertionError")));
        final @NotNull Map<TestRunConfiguration, String> configuration = everyQuestionAnswered();
        configuration.put(TestRunConfiguration.TEST_TYPE, TestRunConfiguration.TEST_TYPE.getOptions()[1]);
        indexedTestRuns().changeTestRunMarker(source.getPath(), marker -> marker.configure(configuration));

        new ReCreateTestRunWork(getProject()).reCreateAt(new TreePath(new Object[]{new DefaultMutableTreeNode(sprint), new DefaultMutableTreeNode(source)}));
        Await.until("the re-create dialog never opened", () -> OnScreenDialog.isOpen(getProject(), TestRunConfigurationDialog.class));
        OnScreenDialog.sized(getProject(), TestRunConfigurationDialog.class);
        theButton(Bundle.message("test.run.create.button")).doClick();

        Await.until("the new test run was not created in the folder of the one it was made from", () -> theTestRunsUnder(sprint).size() == 2);
        final @NotNull DirectoryDto made = theTestRunsUnder(sprint).stream().filter(node -> !node.getPath().equals(source.getPath())).findFirst().orElseThrow();
        Await.until("the new test run holds no test case", () -> !indexedTestRuns().getTestRunByPath(made.getPath()).coveredIds().isEmpty());
        final @NotNull TestRunItems carried = indexedTestRuns().getTestRunByPath(made.getPath()).resultOf(first.getId()).orElseThrow(() -> new AssertionError("the test case was not carried over"));

        assertEquals("the run item status was carried over", RunItemStatus.PENDING, carried.getStatus());
        assertEquals("the duration was carried over", Duration.ZERO, carried.getDuration());
        assertEquals("the failure was carried over", "", carried.getActualResult());
        assertEquals("the failure was carried over", "", carried.getStacktrace());
        assertEquals("the configuration was not carried over", TestRunConfiguration.TEST_TYPE.getOptions()[1], TestRunConfiguration.TEST_TYPE.valueIn(((TestRunDirectoryDto) made).getMarker()));
    }
}
