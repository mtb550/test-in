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
package org.testin.view;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorFixtures;
import org.testin.editor.UnifiedFileEditor;
import org.testin.editor.UnifiedVirtualFile;
import org.testin.editor.testcase.TestCaseEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Bundle;

import java.util.List;

import static org.testin.view.Drawn.holds;

public class FollowSelectionIdeTest extends AbstractViewPanelIdeTest {

    private @NotNull TestCaseEditor anEditorOn(final @NotNull TestSetDirectoryDto ts) {
        return EditorFixtures.openTestCaseEditor(getProject(), ts, getTestRootDisposable());
    }

    private void selectIn(final @NotNull TestCaseEditor editor, final @NotNull TestCaseDto tc) {
        editor.selectWhenLoaded(tc.getId());
        settled();
    }

    // Rule-VIEW-PANEL-015
    public void testMovingTheSelectionFillsThePanelOnlyWhileItIsOnScreen() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto first = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestCaseDto second = aTestCase(ts, "Log in with a locked user", "b");
        final @NotNull TestCaseDto third = aTestCase(ts, "Log in with no password", "c");
        final @NotNull TestCaseEditor editor = anEditorOn(ts);

        view.getPanel().show(List.of(first), ts.getPath2());
        selectIn(editor, second);
        assertTrue("moving the selection did not fill the panel on screen: " + details(), holds(details(), "Log in with a locked user"));

        view.closedByTheTester();
        selectIn(editor, third);
        assertFalse("moving the selection filled a panel that was off the screen: " + details(), holds(details(), "Log in with no password"));
    }

    // Rule-VIEW-PANEL-102
    public void testMovingTheSelectionKeepsTheTabInFront() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto first = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestCaseDto second = aTestCase(ts, "Log in with a locked user", "b");
        final @NotNull TestCaseEditor editor = anEditorOn(ts);

        view.getPanel().show(List.of(first), ts.getPath2());
        view.bringToFront(ViewTab.HISTORY);
        selectIn(editor, second);

        assertEquals("moving the selection took the panel off History", ViewTab.HISTORY.getDisplayName(), view.tabInFront());
        assertTrue("the panel did not follow the selection: " + details(), holds(details(), "Log in with a locked user"));
    }

    // Rule-VIEW-PANEL-016, Rule-VIEW-PANEL-059
    public void testFollowingNeverOpensAPanelTheTesterClosed() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto first = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestCaseDto second = aTestCase(ts, "Log in with a locked user", "b");
        final @NotNull TestCaseEditor editor = anEditorOn(ts);

        view.getPanel().show(List.of(first), ts.getPath2());
        view.closedByTheTester();
        selectIn(editor, second);
        selectIn(editor, first);

        assertFalse("the next click brought back a panel the tester closed", view.isOpen());
    }

    // Rule-VIEW-PANEL-017
    public void testMovingToATestinEditorWithNothingSelectedEmptiesThePanel() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto tc = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestSetDirectoryDto other = aTestSet("Logout");
        aTestCase(other, "Log out from the menu", "a");
        final @NotNull TestCaseEditor nothingSelected = anEditorOn(other);

        view.getPanel().show(List.of(tc), ts.getPath2());
        new UnifiedFileEditor(getProject(), new UnifiedVirtualFile(other), nothingSelected).selectNotify();

        assertEquals("moving to an editor with nothing selected left a test case in the panel", List.of(Bundle.message("details.placeholder")), details());
    }

    // Rule-VIEW-PANEL-009, Rule-VIEW-PANEL-060
    public void testClosingTheEditorTheTestCaseCameFromEmptiesAndClosesThePanel() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto tc = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestSetDirectoryDto other = aTestSet("Logout");
        aTestCase(other, "Log out from the menu", "a");
        final @NotNull TestCaseEditor itsEditor = anEditorOn(ts);
        final @NotNull TestCaseEditor anotherEditor = anEditorOn(other);

        view.getPanel().show(List.of(tc), ts.getPath2());

        Disposer.dispose(anotherEditor);
        assertTrue("closing another editor closed the panel", view.isOpen());
        assertTrue("closing another editor emptied the panel: " + details(), holds(details(), "Log in with a valid user"));

        Disposer.dispose(itsEditor);
        assertEquals("closing the test case's editor left it in the panel", List.of(Bundle.message("details.placeholder")), details());
        assertFalse("closing the test case's editor left the panel open", view.isOpen());
    }
}
