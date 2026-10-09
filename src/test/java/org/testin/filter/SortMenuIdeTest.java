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

package org.testin.filter;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class SortMenuIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull SortPopupBtn sortOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return editor.getToolBar().getToolbarItem(SortPopupBtn.class);
    }

    private static @NotNull List<String> shown(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return editor.getCurrentTestCases().stream().map(TestCaseDto::getDescription).toList();
    }

    private @NotNull List<String> filesOnDisk() {
        try (final Stream<Path> files = Files.list(testSet.getPath())) {
            return files.sorted().map(file -> file.getFileName() + "@" + file.toFile().lastModified()).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    private @NotNull List<TestCaseDto> lowHighMedium(final @NotNull TestProjectNode tp) {
        testSet = EditorFixtures.testSet(getProject(), tp, "Checkout");
        final @NotNull List<TestCaseDto> made = EditorFixtures.testCases(getProject(), testSet, 3);
        final @NotNull List<Priority> priorities = List.of(Priority.LOW, Priority.HIGH, Priority.MEDIUM);
        for (int i = 0; i < made.size(); i++) {
            final @NotNull TestCaseDto prioritised = made.get(i).edit().priority(priorities.get(i)).build();
            prioritised.setParent(testSet);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), prioritised);
        }
        return made;
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274, Rule-EDITOR-PANEL-277
    public void testSortingRearrangesTheScreenKeepsEveryNumberAndWritesNothing() {
        final @NotNull List<TestCaseDto> made = lowHighMedium(EditorFixtures.testProject(getProject(), root));
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<String> before = filesOnDisk();
        final @NotNull List<Integer> numbersBefore = editor.getAllTestCases().stream().map(editor::positionOf).toList();

        Sorting.choose(sortOf(editor), SortField.PRIORITY.getLabel());
        Sorting.choose(sortOf(editor), SortDirection.DESCENDING.getLabel());

        Await.until("the sort never rearranged the list", () -> shown(editor).equals(List.of(made.get(1).getDescription(), made.get(2).getDescription(), made.getFirst().getDescription())));
        assertEquals("a card's number changed with the sort", numbersBefore, editor.getAllTestCases().stream().map(editor::positionOf).toList());
        assertEquals("the button does not say what the list is sorted by", Bundle.message("sort.button.active", SortField.PRIORITY.getLabel(), SortDirection.DESCENDING.getLabel()), sortOf(editor).getAccessibleContext().getAccessibleName());
        assertEquals("sorting wrote to the test set", before, filesOnDisk());
    }

    // Rule-EDITOR-PANEL-276
    public void testTheRunFieldsAreGrayInATestSetEditorAndWorkInATestRun() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull List<TestCaseDto> made = lowHighMedium(tp);
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull TestRunEditor testRunEditor = EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRun(getProject(), tp, made.stream().map(EditorFixtures::pending).toList()), getTestRootDisposable());
        try {
            for (final SortField field : List.of(SortField.RUN_ITEM_STATUS, SortField.EXECUTED_AT, SortField.DURATION)) {
                final @NotNull Presentation inATestSet = Gestures.updated(getProject(), Sorting.entry(sortOf(testSetEditor), field.getLabel()), testSetEditor.getList());
                assertFalse(field.getLabel() + " sorts a test set editor", inATestSet.isEnabled());
                assertEquals(field.getLabel() + " does not say why it is gray", Bundle.message("filter.run.only"), inATestSet.getDescription());
                assertTrue(field.getLabel() + " does not sort a test run", Gestures.updated(getProject(), Sorting.entry(sortOf(testRunEditor), field.getLabel()), testRunEditor.getList()).isEnabled());
            }
        } finally {
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-278
    public void testAnEditorOpenedAgainStartsOnOrder() {
        lowHighMedium(EditorFixtures.testProject(getProject(), root));
        final @NotNull TestSetEditor first = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        Sorting.choose(sortOf(first), SortField.PRIORITY.getLabel());
        Disposer.dispose(first);

        final @NotNull TestSetEditor again = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());

        assertEquals("the sort was remembered", SortField.ORDER, sortOf(again).sortBy());
        assertEquals("the sort was remembered", SortDirection.ASCENDING, sortOf(again).direction());
    }
}
