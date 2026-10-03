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

package org.testin.rename;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TreeGesture;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.TestinEditor;
import org.testin.editor.TestinEditors;
import org.testin.indexer.Nodes;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;
import org.testin.ui.framework.OnScreenDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JTable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RenameWithEditorsOpenIdeTest extends AbstractOpenEditorsIdeTest {

    private TestSetPackageDirectoryDto payments;
    private TestSetDirectoryDto card;
    private TestSetDirectoryDto login;

    @Override
    public void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectDirectoryDto tp = made.testProject(root.resolve("NAFATH"));
        payments = made.testSetPackage(tp.getTestCasesDirectory(), "Payments");
        card = made.testSet(payments, "Card");
        made.testCase(card);
        login = made.testSet(tp.getTestCasesDirectory(), "Login");
    }

    @Override
    public void tearDown() {
        OnScreenDialog.closed(getProject(), RenameDialog.class);
        super.tearDown();
    }

    private @NotNull TestinEditor editorOn(final @NotNull TestSetDirectoryDto testSet) {
        return Services.getInstance(getProject(), TestinEditors.class).editorFor(testSet).orElseThrow(() -> new AssertionError(testSet.getName() + " has no editor open"));
    }

    private void aCellIsBeingEditedIn(final @NotNull TestSetDirectoryDto testSet) {
        final @NotNull TestinEditor editor = editorOn(testSet);
        Await.until("the editor never loaded", () -> !editor.isLoading());
        ((AbstractTestinEditor<?, ?>) editor).onToolBarSwitchedToGridView();
        final @NotNull JTable grid = Optional.ofNullable(UIUtil.uiTraverser(editor.getComponent()).filter(JTable.class).first()).orElseThrow(() -> new AssertionError("the editor shows no grid"));

        for (int column = 0; column < grid.getColumnCount() && !grid.isEditing(); column++) grid.editCellAt(0, column);
        assertTrue("no cell could be opened for editing", editor.isBusy());
    }

    // Rule-TREE-PANEL-111
    public void testARenameClosesEveryEditorOnTheNodeAndUnderIt() {
        opened(card);
        opened(login);

        TreeGesture.pressed(getProject(), new RenameAction(), List.of(payments));
        OnScreenDialog.typed(getProject(), RenameDialog.class, "Billing");
        OnScreenDialog.pressed(getProject(), RenameDialog.class, Shortcuts.Enter);

        Await.until("the package was not renamed", () -> Services.getInstance(getProject(), Nodes.class).nodeExists(payments.getPath().resolveSibling("Billing")));
        assertTrue("an editor on a test set under the renamed package is still open", openOn(card).isEmpty());
        assertEquals("an editor on a node that was not renamed was closed", 1, openOn(login).size());
    }

    // Rule-TREE-PANEL-111
    public void testRenameIsGrayAndSaysWhyWhileAnEditorUnderTheNodeIsBusy() {
        opened(card);
        aCellIsBeingEditedIn(card);

        for (final DirectoryDto node : List.of(card, payments)) {
            final @NotNull Presentation shown = TreeGesture.updated(getProject(), new RenameAction(), List.of(node));

            assertFalse("Rename is not gray on " + node.getName() + " while a cell under it is being edited", shown.isEnabled());
            assertEquals("Rename does not say why it is gray", Bundle.message("rename.disabled.busy"), Objects.requireNonNullElse(shown.getDescription(), ""));
        }

        assertTrue("Rename is gray on a node with no busy editor under it", TreeGesture.updated(getProject(), new RenameAction(), List.of(login)).isEnabled());
    }
}
