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

package org.testin.editor.statusbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.grid.NotWhileEditing;

import javax.swing.JComponent;

/**
 * Turning the page from the keyboard, one action per {@link PageStep}.
 * <p>
 * It was an abstract class with a one-line subclass under it per direction,
 * which was two files saying one word each while the step beside them already
 * held the title, the icon, the key and the arithmetic. Adding First and Last
 * would have made that four files saying one word each - so the step is a
 * constructor argument now, and a new way of turning the page is a constant in
 * {@link PageStep} and nothing else (#175, C10).
 * <p>
 * <b>Not a menu entry.</b> Paging moves the view and does nothing to the test
 * case the tester right-clicked, so it does not belong on a menu about that test
 * case - and the status bar already draws four arrows, each printing its own
 * key. It was on the menu because that is where its shortcut used to be bound;
 * the two facts had grown together and are apart now.
 * <p>
 * Which is why the binding is here rather than at the call sites: a menu entry
 * reached the grid for free through {@code bindShortcutsTo}, and one that is not
 * on the menu has to say so itself or the keys go quiet the moment a tester
 * switches to grid view.
 */
public class PageAction extends DumbAwareAction {

    private final @NotNull TestinEditor editor;
    private final @NotNull PageStep step;

    private PageAction(final @NotNull TestinEditor editor, final @NotNull PageStep step) {
        super(step.getTooltip(), step.getDescription(), step.getIcon());
        this.editor = editor;
        this.step = step;
    }

    /**
     * UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-103.
     * <p>
     * Every way of turning the page, on the surface that has the keyboard.
     */
    public static void bindTo(final @NotNull TestinEditor editor, final @NotNull JComponent component) {
        for (final PageStep step : PageStep.values()) {
            final @NotNull PageAction action = new PageAction(editor, step);
            action.registerCustomShortcutSet(step.getShortcut().getCustomShortcut(), component);
        }
    }

    /**
     * UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-010.
     * <p>
     * The same four on the grid, refused while a cell is open for editing.
     * <p>
     * A page turned under a half-typed Actual Result loses it, and the IDE
     * dispatches a registered shortcut before the cell editor's own keys - which
     * is the whole reason {@link NotWhileEditing} exists.
     */
    public static void bindToGrid(final @NotNull TestinEditor editor, final @NotNull JBTable table) {
        for (final PageStep step : PageStep.values()) {
            NotWhileEditing.bind(table, new PageAction(editor, step));
        }
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-103
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        editor.stepPage(step.deltaFrom(editor.getCurrentPage(), editor.getTotalPageCount()));
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-102
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(step.isAvailable(editor.getCurrentPage(), editor.getTotalPageCount()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT although update() reads no Swing component: the page state it does
        // read is owned by the EDT - actionPerformed writes currentPage there -
        // so a background read would enable or disable the button from a stale
        // page number (#52).
        return ActionUpdateThread.EDT;
    }
}
