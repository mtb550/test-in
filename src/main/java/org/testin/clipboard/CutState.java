package org.testin.clipboard;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;

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
