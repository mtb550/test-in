package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.RadioSelection;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import com.intellij.util.ui.JBUI;

/**
 * What two testers disagreed about in one test case, and nothing else (#90).
 * <p>
 * Everything the merge could settle is already settled by the time this opens -
 * the fields only one side touched, the audit stamps, the order pointers. What
 * is left is a row per field both sides rewrote, mine beside theirs, which on a
 * real conflict is one or two rows rather than the seventeen a text merge would
 * show.
 */
public final class ResolveConflictDialog extends AbstractFrameworkDialog<DialogButton> {

    /**
     * How much of a value a row shows. A description fits; a steps list does not,
     * and a row that wrapped over four lines would bury the choice next to it.
     */
    private static final int SHOWN = 70;

    private final @NotNull List<TestCaseMerge.Question> questions;
    private final @NotNull List<RadioSelection<Boolean>> answers = new ArrayList<>();
    private final @NotNull Consumer<Set<String>> onResolved;

    /**
     * @param testCase   what the case is called, for the title - a tester
     *                   resolving three conflicts in a row needs to know which
     *                   one they are looking at
     * @param onResolved the fields the tester chose the remote's value for.
     *                   Named rather than numbered, so the caller applies them by
     *                   field and never by row order
     */
    public ResolveConflictDialog(final @NotNull Project p, final @NotNull String testCase, final @NotNull List<TestCaseMerge.Question> questions, final @NotNull Consumer<Set<String>> onResolved) {
        super(p);
        this.questions = questions;
        this.onResolved = onResolved;

        title = "Both Changed " + testCase;

        final @NotNull List<ComponentDialogBase<?>> rows = new ArrayList<>();

        for (final TestCaseMerge.Question question : questions) {
            final @NotNull ComponentDialogBase<RadioSelection<Boolean>> row = ComponentDialogBase.<Boolean>radios(label(question.field()))
                    .option("Mine: " + shortened(question.mine()), Boolean.FALSE)
                    .option("Remote: " + shortened(question.theirs()), Boolean.TRUE)
                    .select(Boolean.FALSE)
                    .build();

            rows.add(row);
            answers.add(row.getComponent());
        }

        final @NotNull ComponentDialogBase<DialogButton> keep = ComponentDialogBase.button("Keep Selected");
        rows.add(keep);

        components = List.copyOf(rows);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Keep Selected", this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(700), JBUI.scale(120 + (60 * questions.size())));
    }

    /**
     * The field as the tester knows it, from the enum that already names every
     * test case field for the editor, the details panel and the importer. A
     * field that enum does not carry keeps its own name rather than being
     * dropped - the merge works on the file, which may hold more than the model
     * does.
     */
    private static @NotNull String label(final @NotNull String jsonField) {
        final @NotNull String constant = jsonField.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);

        for (final TestEditorAttributes attribute : TestEditorAttributes.values()) {
            if (attribute.name().equals(constant)) return attribute.getName();
        }
        return jsonField;
    }

    private static @NotNull String shortened(final @NotNull String value) {
        final @NotNull String oneLine = value.replace('\n', ' ').trim();
        if (oneLine.isEmpty()) return "(empty)";

        return oneLine.length() <= SHOWN ? oneLine : oneLine.substring(0, SHOWN - 1) + "…";
    }

    @Override
    protected void submit() {
        final @NotNull Set<String> takeTheirs = new LinkedHashSet<>();

        for (int i = 0; i < questions.size(); i++) {
            if (answers.get(i).getSelected()) takeTheirs.add(questions.get(i).field());
        }

        onResolved.accept(takeTheirs);
        closeOk();
    }
}
