package org.testin.search;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * What a query matches, out of everything the plugin knows (#29).
 * <p>
 * <b>Entirely from the indexer's objects.</b> The test cases and the nodes are
 * already in memory - the scan put them there - so a search is a pass over two
 * lists and no file is opened, nothing is parsed, and nothing waits. That is the
 * whole reason this can run while somebody types.
 * <p>
 * A test case matches on <em>any</em> of its attributes, read through the
 * extractors {@link TestEditorAttributes} already declares. So the description,
 * the id, the expected result, the steps, the priority, the module, the
 * reference and the rest are all searchable, and an attribute added later
 * becomes searchable by being declared rather than by being listed again here.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Hits {

    /**
     * How many hits are shown.
     * <p>
     * A query of one letter matches thousands, and nobody reads past the first
     * screen of them - they type another letter instead. Capping is also what
     * makes a short query cheap: the scan stops as soon as it has this many.
     */
    private static final int SHOWN = 50;

    /**
     * Everything matching, best first - and with nothing typed, everywhere the
     * tester might want to go.
     * <p>
     * An empty query is not an empty answer. Opening the search and seeing every
     * test set and test run makes it a way of getting around: arrow down, Enter,
     * and a set is open, without going to the tree and finding it there. Typing
     * then narrows what is already on screen, which is how a tester learns what
     * the field does without being told.
     */
    public static @NotNull List<Hit> forQuery(final @NotNull Project p, final @NotNull String query) {
        final @NotNull String wanted = query.trim().toLowerCase(Locale.ROOT);
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        // Nodes first, and by name only: a tester who types a test set's name
        // wants the set, not the ninety cases inside it that mention it.
        final @NotNull List<Hit> found = new ArrayList<>(
                wanted.isEmpty() ? everywhereToGo(indexer) : nodesNamed(indexer, wanted));

        found.addAll(cases(p, indexer, wanted, SHOWN - found.size()));

        return List.copyOf(found);
    }

    /**
     * Everywhere a tester can go, for a search with nothing typed in it yet.
     * <p>
     * The nodes that open in an editor, which is test sets and test runs - and
     * which the node itself declares, so a kind that becomes openable later
     * appears here by saying so. The rest of the tree is left out on purpose: a
     * package or a main folder is somewhere to look through rather than
     * somewhere to go, and a list of all of them is the tree again, in a dialog.
     */
    private static @NotNull List<Hit> everywhereToGo(final @NotNull ProjectIndexer indexer) {
        return indexer.getAllNodes().stream()
                .filter(DirectoryDto::isOpenableInEditor)
                .sorted(Hits::inTreeOrder)
                .limit(SHOWN)
                .map(Hit::of)
                .toList();
    }

    /**
     * Every node the tester has named, packages and folders included - because
     * once they have typed the name, that is the thing they are after.
     */
    private static @NotNull List<Hit> nodesNamed(final @NotNull ProjectIndexer indexer, final @NotNull String wanted) {
        return indexer.getAllNodes().stream()
                .filter(node -> contains(node.getName(), wanted))
                .sorted(Hits::byClosestName)
                .limit(SHOWN)
                .map(Hit::of)
                .toList();
    }

    /**
     * The cases to offer, and none at all until the query is worth running.
     * <p>
     * Nodes are cheap to match - one name each - so they answer from the first
     * keystroke. A case is eighteen attributes, and matching a single letter
     * against every attribute of every case in the project is both the slowest
     * query there is and the least useful: it matches nearly all of them.
     */
    private static @NotNull List<Hit> cases(final @NotNull Project p, final @NotNull ProjectIndexer indexer,
                                            final @NotNull String wanted, final int room) {
        if (room <= 0 || tooShort(wanted)) return List.of();

        return indexer.getAllTestCases().stream()
                .filter(tc -> matches(p, tc, wanted))
                .sorted(byDescriptionMatchThenText(wanted))
                .limit(room)
                .map(Hit::of)
                .toList();
    }

    /**
     * Whether any attribute of this case contains the query.
     * <p>
     * Short-circuits on the first attribute that does, so the common case - a
     * description match - costs one comparison rather than eighteen.
     */
    private static boolean matches(final @NotNull Project p, final @NotNull TestCaseDto tc,
                                   final @NotNull String wanted) {
        for (final TestEditorAttributes attribute : TestEditorAttributes.values()) {
            if (contains(attribute.gridValue(p, tc), wanted)) return true;
        }

        return false;
    }

    /**
     * Whether a query is worth searching test cases with.
     * <p>
     * One character matches almost every case in the project, which is a screen
     * of noise rather than an answer - and it is also the most expensive query
     * there is, being eighteen attributes of every case. The tester is one
     * keystroke away from meaning something, and until then the nodes carry the
     * list on their own.
     */
    static boolean tooShort(final @NotNull String wanted) {
        return wanted.trim().length() < 2;
    }

    /**
     * The order the tree would show them in: by where they live, then by name.
     * <p>
     * For the list with nothing typed, which stands in for the tree - so it
     * reads as the tree does, with a project's sets together and its runs
     * together, rather than as an alphabetical jumble of both.
     */
    static int inTreeOrder(final @NotNull DirectoryDto one, final @NotNull DirectoryDto other) {
        final int byPlace = String.join(" > ", one.getPath2())
                .compareToIgnoreCase(String.join(" > ", other.getPath2()));

        return byPlace != 0 ? byPlace : one.getName().compareToIgnoreCase(other.getName());
    }

    /**
     * Whether a value holds the query, whatever case either is written in.
     * <p>
     * Both sides are lowered, not just the value. The query arrives lowered
     * already on the one path through {@link #forQuery}, so this looks like
     * waste - and it is what stops the method being a trap: a caller who does
     * not know that rule gets a silent no rather than an answer, which is
     * exactly what the test that found this did.
     */
    static boolean contains(final @NotNull String value, final @NotNull String wanted) {
        return value.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT));
    }

    /**
     * Shorter names first, then alphabetically - so a node whose whole name is
     * what was typed sits above one that merely contains it.
     */
    static int byClosestName(final @NotNull DirectoryDto one, final @NotNull DirectoryDto other) {
        final int byLength = Integer.compare(one.getName().length(), other.getName().length());

        return byLength != 0 ? byLength : one.getName().compareToIgnoreCase(other.getName());
    }

    /**
     * Cases whose description matches, before cases that matched on something
     * out of sight.
     * <p>
     * The description is what a tester recognizes a case by. One that matched on
     * a step, an id or a reference is a real hit and belongs in the list, but it
     * looks unrelated at a glance - so it goes below the ones that explain
     * themselves.
     */
    static @NotNull Comparator<TestCaseDto> byDescriptionMatchThenText(final @NotNull String wanted) {
        return Comparator.comparingInt((TestCaseDto tc) -> contains(tc.getDescription(), wanted) ? 0 : 1)
                .thenComparing(TestCaseDto::getDescription, String.CASE_INSENSITIVE_ORDER);
    }
}
