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

package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CutState;
import org.testin.services.Services;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.view.ViewToolWindowFactory;

import java.awt.datatransfer.StringSelection;

public class EscapeAction extends AbstractProjectAction {

    /** One name for the four surfaces, so a rename cannot reach three of them. */
    private static final @NotNull String TITLE = Bundle.message("escape.title");

    /**
     * What ESC does on the surface this action was registered on, chosen by the
     * constructor that was used.
     * <p>
     * It used to be four fields - a tree, its transfer handler, a list and a
     * table - of which exactly one set was filled in, and the action worked out
     * again at every press which constructor had been called (#71).
     */
    private final @NotNull Runnable onEscape;

    public EscapeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        super(p, TITLE, Bundle.message("escape.tree"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> clearTreeTransfer(transferHandler);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), tree);
    }

    public EscapeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, TITLE, Bundle.message("escape.editor"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> stepBack(list::clearSelection);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), list);
    }

    /**
     * UC-VIEW-PANEL-015, Rule-VIEW-PANEL-058.
     * <p>
     * The view panel's own tabs, which had no registration at all.
     * <p>
     * ESC closed the panel from the editor and did nothing from inside it - so a
     * tester who had just pressed {@code F2}, which needs the keyboard in the
     * panel, could not close what they were looking at with the key that closes
     * it everywhere else (#226).
     * <p>
     * The same step back as every other surface, not a special "close" - a
     * pending cut still goes first, and one press still does one thing. What
     * differs is only the last step: there is no selection of its own here to
     * clear, so a press with nothing left to undo does nothing, which is what
     * the tester means by then.
     */
    public EscapeAction(final @NotNull Project p, final @NotNull JBPanel<?> tab) {
        super(p, TITLE, Bundle.message("escape.run"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> stepBack(() -> {
        });
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), tab);
    }

    /**
     * Grid view: same behavior as the list, except while a cell is being edited.
     */
    public EscapeAction(final @NotNull Project p, final @NotNull JBTable table) {
        super(p, TITLE, Bundle.message("escape.grid"), AllIcons.Actions.InlayGear);
        this.onEscape = () -> escapeInGrid(table);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), table);
    }

    // UC-TREE-PANEL-013, UC-EDITOR-PANEL-026, UC-VIEW-PANEL-015
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        onEscape.run();
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    private void clearTreeTransfer(final @NotNull TreeTransferHandler transferHandler) {
        // The handler owns the clipboard it wrote, so it is the one that empties
        // it. ESC used to take the gray off the rows and leave the nodes waiting,
        // so the next Ctrl+V still offered the move the tester had called off.
        transferHandler.clearClipboard();
        Logger.info("Clipboard/Cut state cleared via ESC.");
    }

    // UC-EDITOR-PANEL-026, Rule-EDITOR-PANEL-115
    private void escapeInGrid(final @NotNull JBTable table) {
        // While a cell is open the editor owns ESC: cancel the edit only.
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
            return;
        }

        stepBack(table::clearSelection);
    }

    /**
     * UC-EDITOR-PANEL-026, UC-VIEW-PANEL-015, Rule-EDITOR-PANEL-114, Rule-VIEW-PANEL-058.
     * <p>
     * One step back per press, on any surface that shows test cases: drop a
     * pending cut, then close the details panel, then clear the selection.
     * Clearing a selection that is already empty is what Swing does with it -
     * nothing - so it is not asked about first.
     */
    private void stepBack(final @NotNull Runnable clearSelection) {
        if (dropPendingCut()) return;
        if (hideViewPanelIfVisible()) return;

        clearSelection.run();
    }

    /**
     * UC-EDITOR-PANEL-026, Rule-EDITOR-PANEL-114, Rule-VIEW-PANEL-058.
     * <p>
     * Drops a pending cut, and answers whether there was one to drop - which is
     * what makes it a step rather than something that happens on the way past.
     * It used to return nothing and run unconditionally, so a press with a cut
     * waiting and the panel open did both at once, where the rule and the step
     * table both say one press does one of them.
     * <p>
     * The clipboard is emptied only when Testin put a cut on it. The wipe used
     * to sit outside this question entirely, so every press emptied the IDE's
     * clipboard - a URL from a browser, a stack trace from the run console,
     * anything at all - and nothing said so (#289).
     * <p>
     * A copy is left alone: a copy is meant to be pasted more than once, and a
     * cut is spent. The tree is not quite the same here - its clearClipboard
     * empties whenever the clipboard holds Testin's own nodes, cut or copied -
     * so the two surfaces answer "Escape after a copy" differently. Both are
     * safe, because both ask whether the content is Testin's before touching
     * it; which of the two is right is a decision, not a defect.
     */
    private boolean dropPendingCut() {
        if (!Services.getInstance(p, CutState.class).isCutting()) return false;

        Services.getInstance(p, CutState.class).clear();
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        return true;
    }

    /**
     * True when the view panel was open and has been hidden by this ESC.
     */
    private boolean hideViewPanelIfVisible() {
        return ViewToolWindowFactory.toolWindow(p)
                .filter(ToolWindow::isVisible)
                .map(toolWindow -> {
                    toolWindow.hide(null);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
