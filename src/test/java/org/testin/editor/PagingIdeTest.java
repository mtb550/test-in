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

package org.testin.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Balloons;
import org.testin.editor.statusbar.PageStep;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.testcase.TestCaseEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public class PagingIdeTest extends AbstractTempRootIdeTest {
    private static final int PAGE = 50;
    private static final int LONG_TEST_SET = 120;
    private static final @NotNull String SEARCHED = "number 1";
    private static final int NARROWED_TO = 32;

    private @NotNull String pageSizeBefore = "";

    @Override
    protected void setUp() {
        super.setUp();
        pageSizeBefore = Objects.toString(PropertiesComponent.getInstance().getValue(TestinEditor.PAGE_SIZE_KEY), "");
        PropertiesComponent.getInstance().setValue(TestinEditor.PAGE_SIZE_KEY, String.valueOf(PAGE));
    }

    @Override
    protected void tearDown() {
        if (pageSizeBefore.isEmpty()) PropertiesComponent.getInstance().unsetValue(TestinEditor.PAGE_SIZE_KEY);
        else PropertiesComponent.getInstance().setValue(TestinEditor.PAGE_SIZE_KEY, pageSizeBefore);
        super.tearDown();
    }

    private @NotNull TestSetDirectoryDto aLongTestSet() {
        final @NotNull TestSetDirectoryDto ts = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), ts, LONG_TEST_SET);
        return ts;
    }

    private @NotNull TestCaseEditor opened(final @NotNull TestSetDirectoryDto ts) {
        return EditorFixtures.openTestCaseEditor(getProject(), ts, getTestRootDisposable());
    }

    private static void searchForNumber1(final @NotNull TestCaseEditor editor) {
        editor.getToolBar().getSearchTxt().setText(SEARCHED);
        Await.until("the search for '" + SEARCHED + "' never narrowed the list to " + NARROWED_TO, () -> editor.getCurrentTestCases().size() == NARROWED_TO);
    }

    private static void onPage(final @NotNull AbstractTestinEditor<?, ?> editor, final int page) {
        editor.setCurrentPage(page);
        editor.refreshView();
        assertEquals("the editor did not move to page " + page, page, editor.getCurrentPage());
    }

    private static @NotNull List<String> everyFileIn(final @NotNull Path folder) {
        try (final Stream<Path> files = Files.walk(folder)) {
            return files.filter(Files::isRegularFile).sorted().map(file -> file + "@" + file.toFile().lastModified() + "=" + read(file)).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError(file + " could not be read", ex);
        }
    }

    // Rule-EDITOR-PANEL-101
    public void testTheTestSetIsPagedAfterTheSearchHasNarrowedIt() {
        final @NotNull TestCaseEditor editor = opened(aLongTestSet());
        assertEquals("the whole test set is not three pages", 3, editor.getTotalPageCount());

        searchForNumber1(editor);

        assertEquals("the pages were not counted after the search narrowed the test set", 1, editor.getTotalPageCount());
        assertEquals("the page does not hold every narrowed test case", NARROWED_TO, editor.getList().getModel().getSize());
        for (int i = 0; i < editor.getList().getModel().getSize(); i++)
            assertTrue("a test case the search hides is on the page", editor.getList().getModel().getElementAt(i).getDescription().contains(SEARCHED));
    }

    // Rule-EDITOR-PANEL-102
    public void testAnArrowWithNowhereToGoIsGray() {
        final @NotNull TestCaseEditor editor = opened(aLongTestSet());
        final @NotNull StatusBar bar = editor.getStatusBar();

        assertFalse("First is not gray on the first page", bar.button(PageStep.FIRST).isEnabled());
        assertFalse("Previous is not gray on the first page", bar.button(PageStep.PREVIOUS).isEnabled());
        assertTrue("Next is gray with pages after this one", bar.button(PageStep.NEXT).isEnabled());
        assertTrue("Last is gray with pages after this one", bar.button(PageStep.LAST).isEnabled());

        bar.button(PageStep.LAST).doClick();

        assertEquals(3, editor.getCurrentPage());
        assertTrue("First is gray on the last page", bar.button(PageStep.FIRST).isEnabled());
        assertTrue("Previous is gray on the last page", bar.button(PageStep.PREVIOUS).isEnabled());
        assertFalse("Next is not gray on the last page", bar.button(PageStep.NEXT).isEnabled());
        assertFalse("Last is not gray on the last page", bar.button(PageStep.LAST).isEnabled());
    }

    // Rule-EDITOR-PANEL-103, Rule-EDITOR-PANEL-009
    public void testPagingAndSearchingSayNothingAndChangeNothing() {
        final @NotNull TestSetDirectoryDto ts = aLongTestSet();
        final @NotNull TestCaseEditor editor = opened(ts);
        final @NotNull List<String> before = everyFileIn(ts.getPath());
        final @NotNull Balloons balloons = Balloons.heard(getTestRootDisposable());

        editor.getStatusBar().button(PageStep.NEXT).doClick();
        assertEquals("Next did not turn the page", 2, editor.getCurrentPage());
        editor.getStatusBar().button(PageStep.LAST).doClick();
        editor.getStatusBar().button(PageStep.FIRST).doClick();
        searchForNumber1(editor);

        assertEquals("paging or searching said something", List.of(), balloons.shown());
        assertEquals("paging or searching changed a file", before, everyFileIn(ts.getPath()));
    }

    // Rule-EDITOR-PANEL-092
    public void testSearchingGoesBackToTheFirstPage() {
        final @NotNull TestCaseEditor editor = opened(aLongTestSet());
        onPage(editor, 2);

        editor.getToolBar().getSearchTxt().setText("Test case");
        Await.until("the search never went back to the first page", () -> editor.getCurrentPage() == 1);
    }

    private static void reloadsOntoTheSelectedPage(final @NotNull AbstractTestinEditor<?, ?> editor, final @NotNull BooleanSupplier loaded) {
        onPage(editor, 3);
        editor.getList().setSelectedIndex(5);
        final @NotNull TestCaseDto selected = editor.getList().getSelectedValue();

        editor.reloadData();
        Await.until("the editor never reloaded", loaded);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("reloading did not land on the page that holds the selected test case", 3, editor.getCurrentPage());
        assertEquals("the selected test case is not selected after reloading", Optional.of(selected.getId()), Optional.ofNullable(editor.getList().getSelectedValue()).map(TestCaseDto::getId));
    }

    // Rule-EDITOR-PANEL-104
    public void testReloadingATestSetLandsOnThePageThatHoldsTheSelectedTestCase() {
        final @NotNull TestCaseEditor editor = opened(aLongTestSet());

        reloadsOntoTheSelectedPage(editor, () -> !editor.isLoading() && editor.getList().getModel().getSize() > 0);
    }

    // Rule-EDITOR-PANEL-104
    public void testReloadingATestRunLandsOnThePageThatHoldsTheSelectedTestCase() {
        final @NotNull TestProjectDirectoryDto tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetDirectoryDto ts = EditorFixtures.testSet(getProject(), tp, "Checkout");
        final @NotNull TestRunDirectoryDto tr = EditorFixtures.testRun(getProject(), tp, EditorFixtures.testCases(getProject(), ts, LONG_TEST_SET).stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());

        reloadsOntoTheSelectedPage(editor, () -> editor.run().isPresent() && editor.getList().getModel().getSize() > 0);
    }

    // Rule-EDITOR-PANEL-107
    public void testChangingThePageSizeGoesBackToTheFirstPageAndToTheList() {
        final @NotNull TestCaseEditor editor = opened(aLongTestSet());
        onPage(editor, 2);

        editor.getStatusBar().getPageSizeField().setText("10");
        editor.getStatusBar().getPageSizeField().postActionEvent();

        assertEquals("the page did not take the new size", 10, editor.getPageSize());
        assertEquals("changing the page size did not go back to the first page", 1, editor.getCurrentPage());
        assertEquals("the first page does not hold the new size", 10, editor.getList().getModel().getSize());
        assertSame("the keyboard is not handed back to the list", editor.getList(), editor.getPreferredFocusedComponent());
    }
}
