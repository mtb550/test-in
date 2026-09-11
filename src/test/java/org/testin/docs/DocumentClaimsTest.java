package org.testin.docs;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * What the documents claim about themselves, checked by something other than
 * somebody noticing (#280).
 * <p>
 * {@link RuleNumbersTest} guards the numbering and {@link DifferenceNumbersTest}
 * guards the differences. These are the two claims left that a machine can
 * settle: that a link goes somewhere, and that a count is the count.
 * <p>
 * Both have already gone wrong. The README's total was three behind within a day
 * of {@code tools/add-rule.ps1} being written, because the tool moves a part's
 * Numbering row and knows nothing about the README - and by the time this was
 * written every one of its eight per-part rows was stale as well.
 */
public class DocumentClaimsTest {

    private static final @NotNull Path DOCS = Paths.get("docs");
    private static final @NotNull Path README = DOCS.resolve("README.md");

    /**
     * The page a visitor lands on first, which says the same total in its own
     * words and was the one file nothing checked (#66, finding 57).
     */
    private static final @NotNull Path ROOT_README = Paths.get("README.md");

    /**
     * The vocabulary of saying no, which is where a refusal's words live.
     */
    private static final @NotNull Path REFUSED = Paths.get("src", "main", "java", "org", "testin", "notifications", "Refused.java");

    /**
     * A markdown link, and the target inside its brackets.
     */
    private static final @NotNull Pattern LINK = Pattern.compile("\\[[^]]*]\\(([^)]+)\\)");

    /**
     * A rule where it is written out, which is the only place it says anything.
     * A number that appears in a citation is a reference, not a rule.
     */
    private static final @NotNull Pattern RULE = Pattern.compile("\\*\\*(Rule-([A-Z-]+)-\\d+)\\*\\* —");

    /**
     * The heading that makes a page a use case page.
     */
    private static final @NotNull Pattern USE_CASE = Pattern.compile("^# (UC-[A-Z-]+-\\d+):", Pattern.MULTILINE);

    /**
     * One row of the README's part table: the link to the part, then its use
     * case count and its rule count in the last two columns.
     */
    private static final @NotNull Pattern PART_ROW =
            Pattern.compile("^\\| \\*\\*\\[[^]]+]\\((\\w+)/main\\.md\\)\\*\\* \\|[^|]*\\| (\\d+) \\| (\\d+) \\|$", Pattern.MULTILINE);

    /**
     * The sentence under the table that adds the two columns up.
     */
    /**
     * One refusal: its name and the sentence it says.
     */
    private static final @NotNull Pattern REFUSAL = Pattern.compile("\\n    ([A-Z_]+)\\(\"([^\"]+)\"\\)");

    /** Any run of whitespace, so a sentence wrapped across two lines is one sentence. */
    /** A fenced code block: a screen drawing, a page template, a snippet. */
    private static final @NotNull Pattern FENCED = Pattern.compile("(?s)```.*?```");

    /** An inline code span. Code is not prose, and a link inside it is not a link. */
    private static final @NotNull Pattern INLINE = Pattern.compile("(?s)`[^`]*`");

    private static final @NotNull Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private static final @NotNull Pattern TOTALS = Pattern.compile("(\\d+) use cases and (\\d+) rules");

    /**
     * UC-INTERNAL-006.
     * <p>
     * Every link inside the documents goes somewhere that exists.
     * <p>
     * 1,122 of them across 163 files, cross-linked, and the only check before
     * this was a script somebody ran by hand once. A footer link is on every
     * page, so a folder renamed without its citations breaks a hundred of them
     * at a stroke.
     * <p>
     * Code is skipped, fenced and inline alike - see {@link #withoutCode}.
     * <p>
     * <b>A target outside {@code docs/} is broken however plainly the file is
     * there.</b> Only {@code docs/} is published, so
     * {@code [Contributing](../CONTRIBUTING.md)} resolves against the working
     * tree and passes, and resolves against the site root and serves a 404. Two
     * were written while #99 and #102 landed and both were caught by somebody
     * noticing. Such a link is written as the full repository URL instead.
     */
    @Test
    public void everyInternalLinkGoesSomewhere() {
        final @NotNull List<String> broken = new ArrayList<>();

        for (final Path page : markdownFiles()) {
            final @NotNull Matcher link = LINK.matcher(withoutCode(read(page)));

            while (link.find()) {
                final @NotNull String target = link.group(1);
                if (target.startsWith("http") || target.startsWith("mailto:")) continue;

                // An anchor on the page itself has no file to find. Whether the
                // heading exists is a different question, and a noisier one:
                // GitHub's own slug rules decide it.
                final @NotNull String file = target.contains("#") ? target.substring(0, target.indexOf('#')) : target;
                if (file.isEmpty()) continue;

                final @NotNull Path resolved = page.getParent().resolve(file).normalize();

                if (!resolved.startsWith(DOCS)) {
                    broken.add(DOCS.relativize(page) + " points at " + target + ", which is outside docs/");
                } else if (!Files.exists(resolved)) {
                    broken.add(DOCS.relativize(page) + " points at " + target);
                }
            }
        }

        if (!broken.isEmpty()) {
            fail("These links go nowhere. A citation that cannot be followed is worse than none, "
                    + "because a reader takes it for an answer that exists:\n  " + String.join("\n  ", broken));
        }
    }

    /**
     * UC-INTERNAL-006.
     * <p>
     * The README's part table says how many rules each part writes, and it is
     * the number the part actually writes.
     * <p>
     * Maintained by hand, and {@code tools/add-rule.ps1} moves a part's own
     * Numbering row without touching it - so the table drifts once per rule
     * added and nothing says so. Every one of the eight rows was wrong when this
     * test was written.
     * <p>
     * Counted where a rule is written out, not where its number appears: a
     * citation is a reference, and a retired number is written nowhere and is
     * not a rule any more.
     */
    @Test
    public void theReadmeCountsTheRulesEachPartWrites() {
        assertEquals(readmeRuleCounts(), measuredRuleCounts(),
                "the README's part table disagrees with the parts. Left is what it claims, right is what they write");
    }

    /**
     * UC-INTERNAL-006.
     * <p>
     * The same for the use cases: a part's row is the number of use case pages
     * that part has.
     */
    @Test
    public void theReadmeCountsTheUseCasesEachPartHas() {
        assertEquals(readmeUseCaseCounts(), measuredUseCaseCounts(),
                "the README's part table disagrees with the parts. Left is what it claims, right is what they hold");
    }

    /**
     * UC-INTERNAL-006.
     * <p>
     * The sentence under the table adds its own rows up.
     * <p>
     * Its own, deliberately. A breakdown whose parts do not sum to its total is
     * worse than no breakdown, because the whole table exists to be trusted -
     * and the two tests above already tie the rows to the documents, so tying
     * the total to the rows ties it to everything.
     */
    @Test
    public void theReadmeTotalIsTheSumOfItsOwnRows() {
        final @NotNull Matcher totals = totalsOf(README);

        assertEquals(Integer.parseInt(totals.group(1)), sum(readmeUseCaseCounts()), "use cases: the total is not the sum of the rows");
        assertEquals(Integer.parseInt(totals.group(2)), sum(readmeRuleCounts()), "rules: the total is not the sum of the rows");
    }

    /**
     * UC-INTERNAL-006.
     * <p>
     * The repository front page says the same total, and it is the count the
     * parts actually hold.
     * <p>
     * It is the page a visitor reads first and it was the one nothing checked:
     * the guard opened {@code docs/README.md} and no other file, so the front
     * page was one use case over and three rules under at the same time. It was
     * corrected by hand four times in one day while rules were being added,
     * which is the whole argument for checking it (#66, finding 57).
     * <p>
     * Measured against the parts rather than against the other README, because
     * the parts are what the other README is measured against.
     */
    @Test
    public void theRootReadmeSaysTheSameTotal() {
        final @NotNull Matcher totals = totalsOf(ROOT_README);

        assertEquals(Integer.parseInt(totals.group(1)), sum(measuredUseCaseCounts()), "use cases: the front page is not what the parts hold");
        assertEquals(Integer.parseInt(totals.group(2)), sum(measuredRuleCounts()), "rules: the front page is not what the parts write");
    }

    /**
     * The sentence saying how many use cases and rules there are, on a page that
     * has to carry one.
     */
    private static @NotNull Matcher totalsOf(final @NotNull Path page) {
        final @NotNull Matcher totals = TOTALS.matcher(read(page));
        if (!totals.find()) fail(page + " no longer says how many use cases and rules there are");

        return totals;
    }


    /**
     * UC-INTERNAL-006.
     * <p>
     * Every refusal Testin has words for is written on a page.
     * <p>
     * {@code Refused} is the vocabulary of saying no - nine sentences, each one
     * something a tester can be shown - and a refusal nobody documented is a
     * screen a tester meets with nothing to read about it. This found one on its
     * first run: <i>needs the IDE to finish indexing first</i> was on no page.
     * <p>
     * <b>Only the named ones.</b> There are 56 {@code softRefuse} call sites and
     * most pass their own sentence, composed at the call site from a node name or
     * a count. Matching those against prose is reading, not checking - the thing
     * this test cannot do and should not pretend to. What it can settle is that
     * every sentence with a name has a home.
     * <p>
     * Matched on the longest run of literal text between the slots, with
     * whitespace flattened on both sides: the documents wrap, and a sentence
     * broken across two lines is the same sentence.
     */
    @Test
    public void everyNamedRefusalIsWrittenDown() {
        final @NotNull String prose = flattened(String.join(" ", markdownFiles().stream().map(DocumentClaimsTest::read).toList()));
        final @NotNull List<String> missing = new ArrayList<>();

        final @NotNull Matcher refusal = REFUSAL.matcher(read(REFUSED));
        while (refusal.find()) {
            final @NotNull String longest = longestLiteral(refusal.group(2));

            if (!prose.contains(flattened(longest))) missing.add(refusal.group(1) + " - " + longest);
        }

        if (!missing.isEmpty()) {
            fail("These refusals are on no page. A tester who meets one has nothing to read about it:\n  "
                    + String.join("\n  ", missing));
        }
    }

    /**
     * The longest stretch of a refusal that is words rather than a slot. A
     * sentence is mostly literal and the slot is a name, so the longest piece is
     * the one worth looking for.
     */
    private static @NotNull String longestLiteral(final @NotNull String sentence) {
        @NotNull String longest = "";

        for (final String part : sentence.split("%s")) {
            // Quote characters at an edge belong to the sentence's own
            // punctuation around the slot, not to the words - a repository address
            // is written '%s' and the documents quote it their own way.
            final @NotNull String trimmed = part.trim().replaceAll("^['\"]+|['\"]+$", "");
            if (trimmed.length() > longest.length()) longest = trimmed;
        }

        return longest;
    }

    /**
     * One space where there was any run of whitespace, lower-cased. The
     * documents wrap and the code does not.
     */
    private static @NotNull String flattened(final @NotNull String text) {
        return WHITESPACE.matcher(text).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    private static int sum(final @NotNull Map<String, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static @NotNull Map<String, Integer> readmeRuleCounts() {
        return readmeColumn(3);
    }

    private static @NotNull Map<String, Integer> readmeUseCaseCounts() {
        return readmeColumn(2);
    }

    /**
     * One column of the README's part table, by the folder each row links to.
     */
    private static @NotNull Map<String, Integer> readmeColumn(final int group) {
        final @NotNull Map<String, Integer> counts = new TreeMap<>();
        final @NotNull Matcher row = PART_ROW.matcher(read(README));

        while (row.find()) counts.put(row.group(1), Integer.parseInt(row.group(group)));

        if (counts.isEmpty()) fail("The README's part table has no rows this test can read - has its shape changed?");

        return counts;
    }

    /**
     * How many rules each part's folder writes out.
     */
    private static @NotNull Map<String, Integer> measuredRuleCounts() {
        final @NotNull Map<String, Set<String>> byFolder = new LinkedHashMap<>();

        for (final Path page : markdownFiles()) {
            final @NotNull Path parent = page.getParent();
            if (parent.equals(DOCS)) continue;

            final @NotNull Matcher rule = RULE.matcher(read(page));
            while (rule.find()) {
                byFolder.computeIfAbsent(parent.getFileName().toString(), any -> new TreeSet<>()).add(rule.group(1));
            }
        }

        final @NotNull Map<String, Integer> counts = new TreeMap<>();
        byFolder.forEach((folder, rules) -> counts.put(folder, rules.size()));

        return counts;
    }

    /**
     * How many use case pages each part's folder holds.
     */
    private static @NotNull Map<String, Integer> measuredUseCaseCounts() {
        final @NotNull Map<String, Set<String>> byFolder = new LinkedHashMap<>();

        for (final Path page : markdownFiles()) {
            final @NotNull Path parent = page.getParent();
            if (parent.equals(DOCS)) continue;

            final @NotNull Matcher useCase = USE_CASE.matcher(read(page));
            while (useCase.find()) {
                byFolder.computeIfAbsent(parent.getFileName().toString(), any -> new TreeSet<>()).add(useCase.group(1));
            }
        }

        final @NotNull Map<String, Integer> counts = new TreeMap<>();
        byFolder.forEach((folder, pages) -> counts.put(folder, pages.size()));

        return counts;
    }

    /**
     * The text with code taken out - fenced blocks and inline spans both.
     * <p>
     * Code is not prose and a link inside it is not a link. {@code standard.md}
     * documents what a page looks like, so it writes both: a page template
     * inside a fence, naming {@code main.md} from a folder it is not in, and an
     * example citation in backticks - each exactly right for the page it
     * illustrates and exactly wrong if followed.
     */
    private static @NotNull String withoutCode(final @NotNull String text) {
        return FENCED.matcher(text).replaceAll("").replaceAll(INLINE.pattern(), "");
    }

    private static @NotNull List<Path> markdownFiles() {
        try (Stream<Path> tree = Files.walk(DOCS)) {
            return tree.filter(file -> file.getFileName().toString().endsWith(".md")).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + DOCS + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path page) {
        try {
            return Files.readString(page);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + page + ": " + ex.getMessage(), ex);
        }
    }
}
