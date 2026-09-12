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

package org.testin.docs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.fail;

/**
 * A difference is a number too, and the pages pointing at it have to keep up.
 * <p>
 * Each part's {@code main.md} lists what the plugin does that its own rules
 * forbid, numbered, and the use case pages point back at those numbers - "That
 * is difference 6 on the tree panel page". When one is fixed the row moves to
 * the retired table under it, and nothing makes the pages follow. They keep
 * describing a defect that is gone, which is worse than saying nothing: a
 * tester reads that Testin will lose their work and stops using the feature
 * that was repaired.
 * <p>
 * That is not a guess. Eleven pages were found describing defects fixed earlier
 * the same day, and they were found by reading. The one this test was written
 * around is smaller and shows why a script is needed: {@code keepRemovedNode.md}
 * in the internal part still said a removal that could not be copied aside says
 * nothing on screen, five weeks after it started saying <i>Cannot Be Undone</i> -
 * and the first script written for this missed it, because it looked only for
 * citations of a page's own part and that one points at the tree panel's list.
 * <p>
 * So the link is what says whose difference a citation means. A citation with no
 * link is this page's own part.
 * <p>
 * Written as a scan of the documents for the reason {@code RuleNumbersTest}
 * gives: what has to hold is a rule about the shape of what is written down, and
 * there is nothing to run it against but the files themselves.
 */
public class DifferenceNumbersTest {

    private static final Path DOCS = Paths.get("docs");

    /**
     * The line every part puts between the differences that are live and the
     * ones that have been dealt with. Codegen says "Settled" where the others
     * say "Fixed", because one of its rows was decided not to be a difference at
     * all rather than repaired.
     */
    private static final Pattern RETIRED_FROM_HERE = Pattern.compile(
            "\\*\\*(?:Fixed|Settled) since this list was written\\.\\*\\*");

    /**
     * A row of a differences table. A live one carries the rule it breaks and
     * what a tester sees; a retired one carries what it was and when it went.
     */
    private static final Pattern ROW = Pattern.compile("^\\| \\*\\*Difference (\\d+)\\*\\* \\|(.*)$", Pattern.MULTILINE);

    /**
     * A page pointing at a difference, and the part whose list it means.
     * <p>
     * The link is the part: "difference 6 on [the tree panel page](main.md…)"
     * within a part, or "(../treePanel/main.md…)" from another. A number with no
     * link after it belongs to the page's own part, which is how most of them
     * are written.
     */
    private static final Pattern CITATION = Pattern.compile(
            "difference (\\d+)(?:\\s+on\\s*\\[[^\\]]*\\]\\((?:\\.\\./(\\w+)/)?main\\.md)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RULE_WRITTEN = Pattern.compile("\\*\\*(Rule-[A-Z][A-Z-]*-\\d+)\\*\\*");
    private static final Pattern RULE_NAMED = Pattern.compile("Rule-[A-Z][A-Z-]*-\\d+");

    /**
     * A page must not describe a difference its part has already retired.
     */
    @Test
    public void noPageDescribesADifferenceThatIsGone() {
        final Map<String, Set<Integer>> retired = differences(true);
        final List<String> stale = new ArrayList<>();

        forEachCitation((page, part, number) -> {
            if (retired.getOrDefault(part, Set.of()).contains(number)) {
                stale.add(page + " describes " + part + " difference " + number + ", which that part has retired");
            }
        });

        if (!stale.isEmpty()) {
            fail("These pages describe defects their part says are dealt with. Retiring a difference "
                    + "does not touch the pages that describe it, so this is the only thing that "
                    + "notices:\n  " + String.join("\n  ", stale));
        }
    }

    /**
     * A page must not point at a number its part never had, which is what a
     * citation left behind by a renumbering looks like.
     */
    @Test
    public void everyDifferenceAPagePointsAtIsListed() {
        final Map<String, Set<Integer>> known = differences(false);
        differences(true).forEach((part, numbers) ->
                known.computeIfAbsent(part, p -> new LinkedHashSet<>()).addAll(numbers));

        final List<String> dangling = new ArrayList<>();

        forEachCitation((page, part, number) -> {
            if (!known.getOrDefault(part, Set.of()).contains(number)) {
                dangling.add(page + " points at " + part + " difference " + number + ", which is not on that part's list");
            }
        });

        if (!dangling.isEmpty()) {
            fail("These pages point at differences that are not listed anywhere:\n  " + String.join("\n  ", dangling));
        }
    }

    /**
     * One number, one difference. A row in both tables is a retirement done half
     * way, and the live half goes on being read as current.
     */
    @Test
    public void aDifferenceIsLiveOrRetiredAndNotBoth() {
        final Map<String, Set<Integer>> live = differences(false);
        final Map<String, Set<Integer>> retired = differences(true);

        final List<String> both = new ArrayList<>();
        live.forEach((part, numbers) -> numbers.stream()
                .filter(number -> retired.getOrDefault(part, Set.of()).contains(number))
                .forEach(number -> both.add(part + " difference " + number + " is listed as live and as retired")));

        if (!both.isEmpty()) {
            fail("A difference is one or the other:\n  " + String.join("\n  ", both));
        }
    }

    /**
     * A differences table names the rule each difference breaks, and a rule
     * nobody wrote sends the reader looking for it.
     * <p>
     * Whether it names the <b>right</b> rule is not checked here and cannot be:
     * the cell holds a paraphrase, and judging a paraphrase against a rule is
     * reading, not matching. Six rows were found naming a rule that says
     * something else on 8 September 2026, all by hand.
     */
    @Test
    public void everyRuleADifferenceNamesExists() {
        final Set<String> written = new LinkedHashSet<>();
        for (final Path page : pages()) {
            final Matcher rule = RULE_WRITTEN.matcher(read(page));
            while (rule.find()) written.add(rule.group(1));
        }

        final List<String> dangling = new ArrayList<>();
        for (final Path main : mainPages()) {
            final String live = RETIRED_FROM_HERE.split(read(main))[0];
            final Matcher row = ROW.matcher(live);

            while (row.find()) {
                final Matcher named = RULE_NAMED.matcher(row.group(2));
                while (named.find()) {
                    if (!written.contains(named.group())) {
                        dangling.add(main.getParent().getFileName() + " difference " + row.group(1)
                                + " names " + named.group() + ", which no document writes");
                    }
                }
            }
        }

        if (!dangling.isEmpty()) {
            fail("These differences name rules that do not exist:\n  " + String.join("\n  ", dangling));
        }
    }

    /**
     * What a page said about a difference, and whose difference it is.
     */
    private interface Citation {
        void found(String page, String part, int number);
    }

    /**
     * Every pointer at a difference, on every page.
     * <p>
     * A part's front page is read too, with its own two tables taken out first.
     * It used to be skipped whole, because every row of those tables reads as a
     * citation of itself - and the cost of that shortcut was the one thing these
     * tests exist to catch: a front page describing a difference another part
     * has retired went unnoticed, which docs/internal/main.md was doing (#66,
     * finding 99).
     */
    private static void forEachCitation(final Citation tell) {
        for (final Path page : pages()) {
            final String part = page.getParent().getFileName().toString();

            final Matcher cited = CITATION.matcher(oneLine(withoutDifferenceRows(read(page))));
            while (cited.find()) {
                tell.found(part + "/" + page.getFileName(), cited.group(2) == null ? part : cited.group(2),
                        Integer.parseInt(cited.group(1)));
            }
        }
    }

    /**
     * The page with its differences tables taken out, so what is left is the
     * prose that points at them.
     */
    private static String withoutDifferenceRows(final String text) {
        return ROW.matcher(text).replaceAll("");
    }

    /**
     * The difference numbers each part lists, on one side of the retirement line
     * or the other.
     */
    private static Map<String, Set<Integer>> differences(final boolean gone) {
        final Map<String, Set<Integer>> byPart = new TreeMap<>();

        for (final Path main : mainPages()) {
            final String[] halves = RETIRED_FROM_HERE.split(read(main));
            final String half = gone ? String.join("", List.of(halves).subList(Math.min(1, halves.length), halves.length))
                    : halves[0];

            final Matcher row = ROW.matcher(half);
            final Set<Integer> numbers = new LinkedHashSet<>();
            while (row.find()) numbers.add(Integer.parseInt(row.group(1)));

            byPart.put(main.getParent().getFileName().toString(), numbers);
        }

        return byPart;
    }

    /**
     * A citation can wrap onto the next line between the number and the link
     * that says whose it is, so the page is read as one line before matching.
     */
    private static String oneLine(final String text) {
        return text.replaceAll("\\s+", " ");
    }

    private static List<Path> mainPages() {
        return pages().stream().filter(page -> "main.md".equals(page.getFileName().toString())).toList();
    }

    /**
     * Every page inside a part. The pages at the top of {@code docs} belong to
     * no part and list no differences.
     */
    private static List<Path> pages() {
        final List<Path> pages = new ArrayList<>();

        try (Stream<Path> tree = Files.walk(DOCS)) {
            for (final Path file : tree.toList()) {
                if (!file.toString().endsWith(".md")) continue;
                if (file.getParent().equals(DOCS)) continue;

                pages.add(file);
            }
        } catch (final IOException ex) {
            fail("Could not read " + DOCS + ": " + ex.getMessage());
        }

        return pages;
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            fail("Could not read " + file + ": " + ex.getMessage());
            return "";
        }
    }
}
