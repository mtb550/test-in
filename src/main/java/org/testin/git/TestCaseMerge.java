package org.testin.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Mapper;
import org.testin.util.TestDataParser;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Merges the three versions Git keeps of a conflicted test case, field by field
 * (#90).
 * <p>
 * A conflict here is not a text conflict. It is two testers touching the same
 * test case, and the file it lands in is machine-written JSON with one field per
 * line - so a line-based merge asks about the lines that happen to differ rather
 * than about the disagreement. The lines that most often differ are the ones
 * nobody should ever be asked about: both sides stamp {@code updatedAt} on every
 * edit, and both rewrite {@code next} when they add a case to the same test set.
 * <p>
 * Field by field, the ordinary three-way rule settles almost everything: a field
 * one side left alone takes the other side's value, and a field both sides set
 * the same way was never a disagreement. What is left - both sides changed it,
 * differently - is either decided by a rule of its own or handed to the tester.
 * <p>
 * Over JSON rather than over {@code TestCaseDto}: the file is the thing Git
 * conflicted on, a field added to the model later merges without being added
 * here, and nothing has to be constructed from a half-read side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseMerge {

    /**
     * Who last touched the case, which both sides always rewrite. Settled by
     * taking the later edit rather than by asking - the question has no meaning
     * to the tester and its answer is in the two timestamps.
     */
    private static final @NotNull String UPDATED_AT = "updatedAt";
    private static final @NotNull String UPDATED_BY = "updatedBy";

    /**
     * Where the case sits in its test set. Both testers can have moved it, and
     * "which position" is not a question either of them can usefully answer
     * about a merge - so the remote's rank is taken, and the case is one place
     * from where the other tester left it rather than missing.
     */
    private static final @NotNull Set<String> ORDER = Set.of("order");

    /**
     * Never a real conflict, and never worth a question: the file is the case,
     * so both sides carry the same id, and creation happened once before either
     * of them.
     */
    private static final @NotNull Set<String> SETTLED = Set.of("id", "createdAt", "createdBy");

    /**
     * The merged case, and what could not be merged without asking.
     *
     * @param merged    every field settled so far - complete when
     *                  {@code questions} is empty
     * @param questions the fields both sides changed to different values, in the
     *                  order the file lists them
     */
    public record Merge(@NotNull ObjectNode merged, @NotNull List<Question> questions) {

        public boolean isSettled() {
            return questions.isEmpty();
        }
    }

    /**
     * One field two testers disagreed about.
     *
     * @param field  the JSON name, which is the field name a tester reads in the
     *               editor
     * @param mine   the value on this machine, as text
     * @param theirs the value the remote brought, as text
     */
    public record Question(@NotNull String field, @NotNull String mine, @NotNull String theirs) {
    }

    /**
     * Merges the three stages Git holds.
     *
     * @param mapper the project's mapper - handed in rather than reached for, so
     *               the merge rules can be asserted without an IDE
     * @param base   the common ancestor, or empty text when there is none. Two
     *               testers who created a case under the same name share no
     *               past, so every field reads as set by both - which is the
     *               honest answer
     * @param mine   this machine's version
     * @param theirs the version the pull brought
     */
    public static @NotNull Merge of(final @NotNull Mapper mapper, final @NotNull String base,
                                    final @NotNull String mine, final @NotNull String theirs) {
        final ObjectNode baseNode = mapper.readTree(base);
        final ObjectNode mineNode = mapper.readTree(mine);
        final ObjectNode theirsNode = mapper.readTree(theirs);

        final ObjectNode merged = mineNode.deepCopy();
        final List<Question> questions = new ArrayList<>();

        for (final String field : fields(mineNode, theirsNode)) {
            final JsonNode was = baseNode.get(field);
            final JsonNode ours = mineNode.get(field);
            final JsonNode yours = theirsNode.get(field);

            if (same(ours, yours)) continue;

            // The ordinary three-way rule, and it settles most of a test case:
            // a field one side never touched takes the other side's value.
            if (same(was, ours)) {
                set(merged, field, yours);
                continue;
            }
            if (same(was, yours)) {
                set(merged, field, ours);
                continue;
            }

            if (UPDATED_AT.equals(field) || UPDATED_BY.equals(field)) continue;
            if (ORDER.contains(field)) {
                set(merged, field, yours);
                continue;
            }
            if (SETTLED.contains(field)) continue;

            questions.add(new Question(field, text(ours), text(yours)));
        }

        stampTheLaterEdit(merged, mineNode, theirsNode);

        return new Merge(merged, List.copyOf(questions));
    }

    /**
     * Takes the tester's answer for one field.
     */
    public static void answer(final @NotNull Mapper mapper, final @NotNull ObjectNode merged,
                              final @NotNull Question question, final boolean takeTheirs,
                              final @NotNull String theirs) {
        if (!takeTheirs) return;

        set(merged, question.field(), mapper.readTree(theirs).get(question.field()));
    }

    /**
     * Who edited last, by the two stamps rather than by which side Git called
     * ours. Both are rewritten on every edit, so this is the one pair that
     * conflicts even when the testers agreed about everything else.
     */
    private static void stampTheLaterEdit(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine,
                                          final @NotNull ObjectNode theirs) {
        final ZonedDateTime mineAt = TestDataParser.date(text(mine.get(UPDATED_AT)));
        final ZonedDateTime theirsAt = TestDataParser.date(text(theirs.get(UPDATED_AT)));

        final ObjectNode later = theirsAt.isAfter(mineAt) ? theirs : mine;

        set(merged, UPDATED_AT, later.get(UPDATED_AT));
        set(merged, UPDATED_BY, later.get(UPDATED_BY));
    }

    /**
     * Every field either side has, in the order the file lists them, so a
     * question sequence reads like the file does.
     */
    private static @NotNull Set<String> fields(final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs) {
        final Set<String> names = new LinkedHashSet<>();
        mine.fieldNames().forEachRemaining(names::add);
        theirs.fieldNames().forEachRemaining(names::add);

        return names;
    }

    private static void set(final @NotNull ObjectNode target, final @NotNull String field, final JsonNode value) {
        if (value == null) target.remove(field);
        else target.set(field, value);
    }

    /**
     * Two values Git would call different but a tester would not: an absent
     * field and a null one say the same thing about a test case.
     */
    private static boolean same(final JsonNode one, final JsonNode other) {
        final boolean oneEmpty = one == null || one.isNull();
        final boolean otherEmpty = other == null || other.isNull();

        if (oneEmpty || otherEmpty) return oneEmpty == otherEmpty;

        return one.equals(other);
    }

    /**
     * A value as the tester should read it in a question: the text of a string,
     * and the JSON of anything with structure - a list of steps says more as
     * {@code ["open the app", "sign in"]} than as a class name.
     */
    private static @NotNull String text(final JsonNode value) {
        if (value == null || value.isNull()) return "";

        return value.isValueNode() ? value.asText() : value.toString();
    }

}
