package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.testcase.TestEditorAttributes;
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
    private final @NotNull Runnable onSkipped;

    /**
     * @param testCase   what the case is called, for the title - a tester
     *                   resolving three conflicts in a row needs to know which
     *                   one they are looking at
     * @param onResolved the fields the tester chose the remote's value for.
     *                   Named rather than numbered, so the caller applies them by
     *                   field and never by row order
     */
    public ResolveConflictDialog(final @NotNull Project p, final @NotNull String testCase, final @NotNull List<TestCaseMerge.Question> questions, final @NotNull List<String> settled, final @NotNull Consumer<Set<String>> onResolved, final @NotNull Runnable onSkipped) {
        super(p);
        this.questions = questions;
        this.onResolved = onResolved;
        this.onSkipped = onSkipped;

        title = Bundle.message("dialog.conflict.title", testCase);

        final @NotNull List<ComponentDialogBase<?>> rows = new ArrayList<>();

        // Said here, in the dialog that is already about this test case, rather
        // than in a message afterwards: the tester is looking at the decisions
        // they are being asked to make, and these are the ones that were made
        // for them. Nothing is shown when nothing was settled (#261).
        if (!settled.isEmpty()) rows.add(ComponentDialogBase.message(settledSentence(settled)));

        for (final TestCaseMerge.Question question : questions) {
            final @NotNull ComponentDialogBase<RadioSelection<Boolean>> row = ComponentDialogBase.<Boolean>radios(label(question.field()))
                    .option(Bundle.message("dialog.conflict.option.mine", shortened(question.mine())), Boolean.FALSE)
                    .option(Bundle.message("dialog.conflict.option.remote", shortened(question.theirs())), Boolean.TRUE)
                    .select(Boolean.FALSE)
                    .build();

            rows.add(row);
            answers.add(row.getComponent());
        }

        final @NotNull ComponentDialogBase<DialogButton> keep = ComponentDialogBase.button(Bundle.message("dialog.conflict.button.keep"));
        rows.add(keep);

        components = List.copyOf(rows);

        // Skip rather than Cancel, and it says so on the status bar. Escape
        // used to end the whole sync: the rest of the conflicting test cases
        // were never asked about, and the tester was left mid-rebase with no
        // word about any of it (#258).
        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.conflict.button.keep"), this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("dialog.conflict.shortcut.skip"), this::skip));

        preferredSize = new Dimension(JBUI.scale(700), JBUI.scale(120 + (60 * questions.size())));
    }

    /**
     * UC-SHARE-017, Rule-SHARE-109.
     * <p>
     * What the merge decided without asking, named.
     * <p>
     * The reason comes with it, because the fields differ in why they were not
     * a question: who changed the case last has an answer in the two timestamps
     * and none the tester could give, and a position is not something either of
     * them can usefully choose about a merge.
     */
    private static @NotNull String settledSentence(final @NotNull List<String> settled) {
        final @NotNull List<String> named = settled.stream().map(ResolveConflictDialog::label).toList();

        return named.size() == 1
                ? Bundle.message("dialog.conflict.settled.one", Display.andJoin(named))
                : Bundle.message("dialog.conflict.settled.many", Display.andJoin(named));
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

    // UC-SHARE-018, Rule-SHARE-083
    private static @NotNull String shortened(final @NotNull String value) {
        final @NotNull String oneLine = value.replace('\n', ' ').trim();
        if (oneLine.isEmpty()) return Bundle.message("dialog.conflict.empty");

        return oneLine.length() <= SHOWN ? oneLine : oneLine.substring(0, SHOWN - 1) + "…";
    }

    /**
     * UC-SHARE-018, Rule-SHARE-084.
     * <p>
     * This test case is left as Git has it, and the sync goes on to the next
     * one. What the tester already answered is written and staged before each
     * question closes, so it stays; this one is reported at the end with
     * everything else that could not be resolved here.
     */
    private void skip() {
        closeCancel();
        onSkipped.run();
    }

    // UC-SHARE-018
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
