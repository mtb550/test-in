package org.testin.editor.listeners;

import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.util.Bundle;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Writes a test grid edit into the test case, and regenerates the automation
 * code for the field that changed.
 * <p>
 * The guards, and saying the edit landed, are
 * {@link AbstractGridEditListener}'s - shared with the run grid, which needs
 * both for the same reasons and used to carry its own copy of them.
 */
public class GridEditListener extends AbstractGridEditListener {

    private final @NotNull Runnable onEdited;

    /**
     * The test set this grid belongs to; the grid never mixes test sets.
     */
    private final @NotNull Path testSetPath;

    public GridEditListener(final @NotNull Project p, final @NotNull List<TestCaseDto> pageItems, final @NotNull Runnable onEdited, final @NotNull Path testSetPath) {
        super(p, pageItems);
        this.onEdited = onEdited;
        this.testSetPath = testSetPath;
    }

    @Override
    protected int columnCount() {
        return TestEditorAttributes.values().length;
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-050
    @Override
    protected boolean apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto tc, final int row, final int col) {
        final @NotNull TestEditorAttributes attr = TestEditorAttributes.values()[col];

        // The table model refuses these columns already; asked again of the same
        // attribute because a programmatic setValueAt never goes through the
        // model's answer.
        if (!attr.can(Can.EDIT)) return false;

        // Taken first of all: the setter below writes into the DTO the index is
        // holding, so a snapshot after it would be a snapshot of the edit.
        final @NotNull TestCaseSnapshot undoFrom = TestCaseSnapshot.of(p, testSetPath, List.of(tc.getId()));

        final @NotNull String typed = String.valueOf(model.getValueAt(row, col));

        final @NotNull Object before = attr.gridValue(tc);
        final boolean took = attr.getImportSetter().execute(p, tc, typed);
        final @NotNull Object after = attr.gridValue(tc);

        // Rule-EDITOR-PANEL-206. The cell redraws with the old value either way,
        // so without this a refused typo and an edit that changed nothing look
        // identical to the tester (#204).
        if (!took) Services.getInstance(p, Notifier.class).softRefuse(p, Refused.UNREADABLE, quoted(typed, attr));

        // Always write the normalized value back to the cell - it renumbers
        // steps and drops blank entries even when nothing really changed. That
        // the cell may now differ from what was typed is said by the parent,
        // which both grids run through (#203).
        model.setValueAt(after, row, col);

        if (Objects.equals(before, after)) return false;

        persistAndGenerate(tc, attr, undoFrom);
        onEdited.run();

        return true;
    }

    /**
     * What the refusal names: the text that could not be read, and the column it
     * was typed into. The column matters - a grid is eighteen of them, and
     * "Urgent" is refused by Priority and taken by Module.
     */
    private static @NotNull String quoted(final @NotNull String typed, final @NotNull TestEditorAttributes attr) {
        return Bundle.message("grid.refused.as", typed.trim(), attr.getName());
    }

    /**
     * UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053.
     * <p>
     * Same behavior as the update dialog: write the test case JSON and update the
     * generated automation code for the edited attribute. Runs off the EDT — the
     * code generators schedule their own write command actions.
     */
    private void persistAndGenerate(final @NotNull TestCaseDto tc, final @NotNull TestEditorAttributes attr, final @NotNull TestCaseSnapshot undoFrom) {
        if (testSetPath.toString().isEmpty()) {
            Logger.warn("[grid] edit not persisted - the editor has no test set path");
            return;
        }

        final @NotNull GenType generator = attr.getGenType();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).putTestCase(testSetPath, tc);
            generator.getAction().execute(p, tc);

            TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.edit"), List.of(tc)), undoFrom, TestCaseSnapshot.of(p, testSetPath, List.of(tc.getId())));
        });
    }
}
