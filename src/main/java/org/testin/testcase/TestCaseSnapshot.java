package org.testin.testcase;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoService;
import org.testin.util.EditorUtil;
import org.testin.util.Mapper;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What a handful of test cases looked like at one moment: the ones that were
 * there, and the ids that were not. Applying it makes that moment true again,
 * which is the whole of undo and redo for test data.
 * <p>
 * Absence is a list of ids rather than a null or a placeholder case, so taking
 * back a creation and taking back an edit are one piece of code: one puts a
 * case back, the other takes one away, and no reader asks which it is. That is
 * also why every gesture takes the same snapshot - the ids it is about to
 * touch, before and after - instead of a record shaped like the gesture.
 * <p>
 * The cases are deep copies. Every write path hands the indexer the DTO it is
 * already holding and the indexer keeps that object, so a snapshot of
 * references would be a snapshot of whatever the change is about to do to them.
 * <p>
 * A case appearing or disappearing takes its generated method with it, the way
 * the create and remove actions do. Restoring the JSON alone left the set and
 * the generated class disagreeing - the case back on the card, no method to run
 * it - until something else happened to regenerate. What a case's fields
 * generate is a different question and stays with #153.
 */
public record TestCaseSnapshot(@NotNull Project p, @NotNull Path testSetPath, @NotNull List<TestCaseDto> present, @NotNull List<UUID> absent) {

    /**
     * How these ids stand in the index right now.
     * <p>
     * Called twice around a change - once before, once after - and the pair is
     * the operation. An id the index has no case for lands in {@code absent},
     * which is what a case that has not been created yet, or has just been
     * removed, looks like.
     */
    public static @NotNull TestCaseSnapshot of(final @NotNull Project p, final @NotNull Path testSetPath, final @NotNull List<UUID> ids) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull List<TestCaseDto> present = new ArrayList<>(ids.size());
        final @NotNull List<UUID> absent = new ArrayList<>();

        for (final UUID id : ids)
            indexer.findTestCase(id).ifPresentOrElse(tc -> present.add(copy(p, tc)), () -> absent.add(id));

        return new TestCaseSnapshot(p, testSetPath, present, absent);
    }

    /**
     * How a gesture over these cases is named in the undo menu - "Undo Remove
     * 'Log in with a valid user'", "Undo Update 4 test cases". The case's own
     * words when there is one, because that is what the tester recognizes it
     * by, and a count when there are more of them than a sentence would hold.
     */
    public static @NotNull String describe(final @NotNull String verb, final @NotNull List<TestCaseDto> cases) {
        return cases.size() == 1 ? verb + " '" + cases.getFirst().getDescription() + "'" : verb + " " + cases.size() + " test cases";
    }

    /**
     * The ids of a selection, which is what every call site has in its hand.
     */
    public static @NotNull List<UUID> idsOf(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().map(TestCaseDto::getId).toList();
    }

    /**
     * Records a change that has already happened, so CTRL+Z can put it back.
     * <p>
     * One operation for the whole gesture, whatever its size: a bulk edit over
     * forty cases is one press to undo, because the snapshot either side of it
     * covers all forty. Pushing inside the loop is what would make it forty.
     * <p>
     * Whatever is on screen is told afterwards, by path - see
     * {@link #tellTheSurfaces}.
     */
    public static void record(final @NotNull Project p, final @NotNull String description, final @NotNull TestCaseSnapshot before, final @NotNull TestCaseSnapshot after) {
        record(p, UndoScope.of(before.testSetPath()), description, List.of(before), List.of(after));
    }

    /**
     * The same, for a gesture that touches more than one test set - a cut in
     * one and a paste into another is a single press of CTRL+Z, so it is a
     * single operation over both sets rather than one operation each. It lands
     * in the editor the tester made the gesture in, which is the one they will
     * press the key in.
     */
    public static void record(final @NotNull Project p, final @NotNull UndoScope scope, final @NotNull String description, final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after) {
        // Nothing changed, so there is nothing to take back - and pushing it
        // anyway would spend a press of CTRL+Z on a gesture that did nothing
        // while dropping a real one off the end of a bounded stack.
        if (same(before, after)) return;

        // Pushed on the EDT, once, here: the two actions read the stacks from
        // update(), which the platform runs on the EDT, and a grid cell
        // persists from a pooled thread. Said in one place so that no call site
        // has to remember which thread it is on.
        ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, UndoService.class).push(scope, new UndoService.Operation(
                description,
                () -> restore(p, before, after),
                () -> restore(p, after, before))));
    }

    /**
     * Puts a moment back, unless something else has changed the cases since.
     * <p>
     * {@code expected} is how they stood when this operation was recorded. A
     * sync, a Git pull or another IDE may have written over them in the
     * meantime, and their work is not this operation's to overwrite - so it
     * refuses and says so rather than writing.
     */
    private static void restore(final @NotNull Project p, final @NotNull List<TestCaseSnapshot> target, final @NotNull List<TestCaseSnapshot> expected) {
        if (!expected.stream().allMatch(TestCaseSnapshot::stillStands)) {
            Services.getInstance(p, Notifier.class).softRefuse(p,
                    "These test cases changed since",
                    "Something else has written them - a sync, a pull, or another IDE - so taking this back would write over work that is not yours. Nothing was changed.");
            return;
        }

        target.forEach(TestCaseSnapshot::applyTo);
        tellTheSurfaces(p, target);
    }

    /**
     * Brings whatever is on screen back in line with what was just written: the
     * editor open on each set, and the details panel when it is showing one of
     * these cases.
     * <p>
     * Found by path, not held. Every call site used to hand over its own refresh,
     * which in practice meant handing over the editor it was standing in - and an
     * editor closed and reopened between the change and the undo is a different
     * object. The old one is disposed, its toolbar emptied with it, so reloading
     * it threw on the first item it asked for.
     * <p>
     * The data only, so the filters and the search the tester narrowed the view
     * with survive an undo. Refresh drops those because a tester pressing Refresh
     * means it; nobody pressing CTRL+Z does.
     */
    private static void tellTheSurfaces(final @NotNull Project p, final @NotNull List<TestCaseSnapshot> written) {
        final @NotNull EditorUtil editors = Services.getInstance(p, EditorUtil.class);

        written.forEach(snapshot -> editors.reloadOpen(p, snapshot.testSetPath()));

        ViewToolWindowFactory.panel(p).ifPresent(panel -> written.forEach(snapshot -> panel.refreshIfShowing(snapshot.present())));
    }

    private static boolean same(final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after) {
        if (before.size() != after.size()) return false;

        for (int i = 0; i < before.size(); i++)
            if (!before.get(i).sameAs(after.get(i))) return false;

        return true;
    }

    /**
     * Whether the index still holds exactly what this snapshot says it did.
     */
    private boolean stillStands() {
        return sameAs(of(p, testSetPath, ids()));
    }

    /**
     * Every id this snapshot speaks for, present and absent alike - which is
     * how a caller takes the matching snapshot again later.
     */
    public @NotNull List<UUID> ids() {
        final @NotNull List<UUID> ids = new ArrayList<>(idsOf(present));
        ids.addAll(absent);
        return ids;
    }

    /**
     * Compared as the JSON they are stored as, because a test case is a Lombok
     * DTO with no value equality and adding one would change how every map and
     * set in the plugin treats it. The JSON is also exactly what a difference
     * would mean here: the bytes on disk are not what this snapshot took.
     */
    private boolean sameAs(final @NotNull TestCaseSnapshot other) {
        return absent.equals(other.absent) && asJson().equals(other.asJson());
    }

    private @NotNull List<String> asJson() {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        return present.stream().map(mapper::writeValueAsString).sorted().toList();
    }

    private void applyTo() {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        // Verbatim, so the case comes back with the audit it had. Written
        // through putTestCase it would be stamped as modified by whoever
        // pressed CTRL+Z, which says the opposite of what just happened (#164).
        //
        // A copy per write for the same reason the snapshot is a copy: the
        // indexer keeps the object it is given, and this snapshot may be
        // applied again by the next redo.
        present.forEach(tc -> {
            // A case the index has never heard of is one coming back from a
            // removal rather than one being edited back, and only the first
            // needs a method written. Asked before the save, because after it
            // every case is there.
            final boolean isComingBack = indexer.findTestCase(tc.getId()).isEmpty();

            indexer.putTestCaseVerbatim(testSetPath, copy(p, tc));

            if (isComingBack) GenType.CREATE_TEST_CASE.getAction().execute(p, tc);
        });

        // Only what is actually there. An id that is already gone is the state
        // this asks for, and deleting a file twice is a warning in the log for
        // a job already done.
        absent.forEach(id -> indexer.findTestCase(id).ifPresent(tc -> {
            indexer.removeTestCase(testSetPath, id);
            GenType.REMOVE_TEST_CASE.getAction().execute(p, tc);
        }));
    }

    private static @NotNull TestCaseDto copy(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return Services.getInstance(p, Mapper.class).convertValue(tc, TestCaseDto.class);
    }
}
