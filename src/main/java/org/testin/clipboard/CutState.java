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

package org.testin.clipboard;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The test cases waiting to be pasted somewhere else, and the editor they were
 * cut from.
 * <p>
 * A project service rather than the three static fields it used to be. Those
 * were the whole IDE's: a cut in one project was the state a second project's
 * editor read, and one of them held the editor itself - so cutting a few cases
 * and closing the project without pasting kept that editor, and every Swing
 * component under it, for as long as the IDE ran (#66, finding 17).
 * <p>
 * The "a cut is in progress" flag went with them. It was true exactly when there
 * were cases waiting, so the set answers on its own.
 */
@Service(Service.Level.PROJECT)
public final class CutState {

    private final @NotNull Set<UUID> pending = new HashSet<>();

    /**
     * The editor the cases were cut from, so it can be redrawn when the cut is
     * called off - and empty whenever nothing is waiting.
     */
    private @NotNull Optional<TestinEditor> source = Optional.empty();

    /**
     * UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-080.
     * <p>
     * Asks to be told whenever anything is written to the clipboard, so a cut
     * lasts exactly as long as it is on it.
     * <p>
     * There is one system clipboard, so a write anywhere replaces what a paste
     * would put down - a tree copy or cut, a copied grid selection, a copied id
     * badge, a report path copied from a notification, or a copy made in another
     * application entirely. Six writers went straight to the clipboard and left
     * the cards faded, promising a move the paste would then refuse (#312, N3).
     * <p>
     * Asked of the clipboard rather than told by each writer, which is the
     * answer A55 reached for the paste: six callers each remembering a line is
     * six chances to forget, and the seventh writer would not know there was a
     * line to add.
     * <p>
     * Wired from {@code StartupActivity} rather than from the constructor so the
     * state stays a plain object a test can build, and because the platform is
     * what has to be running before anything can be registered on it.
     * <p>
     * A cut records itself <b>after</b> writing the clipboard, or this would
     * call it off as it was being made.
     */
    public static void initClipboardWatch(final @NotNull Project p) {
        final @NotNull CutState state = Services.getInstance(p, CutState.class);

        CopyPasteManager.getInstance().addContentChangedListener((before, now) -> state.clear(), p);
    }

    /**
     * Marks these cases as cut from this editor, replacing any earlier cut.
     */
    public void cut(final @NotNull TestinEditor editor, final @NotNull List<TestCaseDto> testCases) {
        pending.clear();
        testCases.forEach(tc -> pending.add(tc.getId()));
        source = Optional.of(editor);
    }

    /**
     * Whether this test case is one of the ones waiting to be pasted - which is
     * what draws it faded.
     */
    public boolean isPending(final @NotNull UUID id) {
        return pending.contains(id);
    }

    /**
     * UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-079, Rule-EDITOR-PANEL-082.
     * <p>
     * Whether these are the cases waiting to be pasted, which is what makes a
     * paste of them a move.
     * <p>
     * Asked of what is on the clipboard rather than answered by "a cut is
     * waiting": the clipboard is the whole IDE's and this state is one
     * project's, so a copy made anywhere after the cut replaced what a paste
     * would put down, and the paste took the cut cases away and put different
     * ones in their place as though they had moved (#312, A55).
     */
    public boolean isCutOf(final @NotNull List<TestCaseDto> testCases) {
        return !testCases.isEmpty() && testCases.stream().allMatch(tc -> pending.contains(tc.getId()));
    }

    /**
     * Whether anything is waiting at all.
     */
    public boolean isCutting() {
        return !pending.isEmpty();
    }

    /**
     * The editor the cases were cut from, empty when nothing is waiting.
     */
    public @NotNull Optional<TestinEditor> source() {
        return source;
    }

    /**
     * Calls the cut off and redraws the editor it was made in, so the faded rows
     * come back. Called by a paste that consumed it, by a copy that replaces it,
     * and by Escape.
     */
    public void clear() {
        pending.clear();
        // map rather than a check: an editor with no focused component of its own
        // is an editor with nothing to redraw.
        source.map(TestinEditor::getPreferredFocusedComponent).ifPresent(JComponent::repaint);
        source = Optional.empty();
    }
}
