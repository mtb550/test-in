package org.testin.docs;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * A rule number is a name, and everything wearing it says the same thing.
 * <p>
 * A rule that holds for a whole part is written out on every page in it, so one
 * number is written many times on purpose - which buys a page that can be read
 * on its own, and costs the risk that two copies drift apart. Reword one and
 * forget the rest and the pages quietly start telling a tester different things
 * about the same rule. Nothing but this notices.
 * <p>
 * The other way the numbering goes wrong is a collision: two different rules
 * given one number, because whoever wrote the second read the wrong last
 * number. That happened on 7 September 2026, an hour after the scheme was
 * settled, and what caught it was a script nobody would have run again. Both
 * failures look the same from here - one number, two texts.
 * <p>
 * Written as a scan of the documents for the reason
 * {@code GridEditConfirmationTest} gives: what has to hold is a rule about the
 * shape of what is written down, and there is nothing to run it against but the
 * files themselves.
 */
public class RuleNumbersTest {

    private static final Path DOCS = Paths.get("docs");

    /**
     * The one page at the top of {@code docs} that writes rules of its own.
     */
    private static final String PRODUCT = "product.md";
    private static final Path SOURCES = Paths.get("src", "main", "java");

    /**
     * A rule being written out: the bullet that states it, and the words that
     * follow up to the next rule or the end of the list.
     * <p>
     * Read from the pages that write rules, which is every page inside a part
     * and the product page. The standard is left out because it shows the form
     * in a template rather than writing a rule.
     */
    private static final Pattern DEFINITION = Pattern.compile(
            "^\\s*-\\s+\\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\*(.*?)(?=\\r?\\n\\s*-\\s+\\*\\*Rule-|\\r?\\n\\r?\\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * The same thing written as a table row, which is how the product page has
     * always held its rules - a column for the number and a column for the
     * words, from when the number was a business requirement id.
     */
    private static final Pattern IN_A_TABLE = Pattern.compile(
            "^\\| \\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\* \\|(.*?)\\|\\s*$", Pattern.MULTILINE);

    /**
     * A rule being named, anywhere at all.
     */
    private static final Pattern REFERENCE = Pattern.compile("Rule-([A-Z][A-Z-]*)-(\\d+)");

    /**
     * The range a part's Numbering row claims its rules cover, which is what
     * anyone writing the next rule reads to choose its number.
     */
    private static final Pattern RANGE = Pattern.compile("Rules are `Rule-([A-Z][A-Z-]*)-\\d+` to `Rule-[A-Z][A-Z-]*-(\\d+)`");

    @Test
    public void everyCopyOfARuleSaysTheSameThing() {
        final Map<String, Map<Integer, Map<String, List<String>>>> byPart = definitions();

        final List<String> disagreements = new ArrayList<>();
        byPart.forEach((part, numbers) -> numbers.forEach((number, texts) -> {
            if (texts.size() == 1) return;

            final StringBuilder said = new StringBuilder(
                    String.format("Rule-%s-%03d is written %d different ways:", part, number, texts.size()));
            texts.forEach((text, pages) -> said.append("\n      on ").append(pages).append(": ").append(text));

            disagreements.add(said.toString());
        }));

        if (!disagreements.isEmpty()) {
            fail("A rule that holds for a whole part is written out on every page in it, and every copy "
                    + "has to say the same thing - a page that disagrees is a page telling a tester "
                    + "something no other page says:\n  " + String.join("\n  ", disagreements));
        }
    }

    /**
     * The row a writer reads to pick the next number has to be the truth, or the
     * next rule takes a number that is already taken.
     */
    @Test
    public void everyPartSaysItsLastNumber() {
        final Map<String, Map<Integer, Map<String, List<String>>>> byPart = definitions();

        for (final Map.Entry<String, Map<Integer, Map<String, List<String>>>> part : byPart.entrySet()) {
            final Path main = numberingPage(part.getKey());
            final Matcher claimed = RANGE.matcher(read(main));

            if (!claimed.find()) fail(main + " has no Numbering row saying the range its rules cover");

            final int highest = part.getValue().keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            assertEquals(Integer.parseInt(claimed.group(2)), highest,
                    main + " says its rules end somewhere other than they do, so the next rule written "
                            + "here would take a number that is already taken");
        }
    }

    /**
     * A marker naming a rule that is not in the documents is worse than no
     * marker, because the next reader trusts it and goes looking.
     */
    @Test
    public void everyRuleTheCodeCitesExists() {
        final Set<String> written = new LinkedHashSet<>();
        definitions().forEach((part, numbers) ->
                numbers.keySet().forEach(number -> written.add(String.format("Rule-%s-%03d", part, number))));

        final List<String> dangling = new ArrayList<>();
        for (final Path source : javaFiles()) {
            final Matcher cited = REFERENCE.matcher(read(source));

            while (cited.find()) {
                if (!written.contains(cited.group())) dangling.add(cited.group() + " in " + source.getFileName());
            }
        }

        if (!dangling.isEmpty()) {
            fail("These markers name rules no document writes:\n  " + String.join("\n  ", dangling));
        }
    }

    /**
     * Every rule the documents write out, by part, then number, then the words
     * used - remembering which pages used each wording, so a disagreement can
     * name both sides of it.
     */
    private static Map<String, Map<Integer, Map<String, List<String>>>> definitions() {
        final Map<String, Map<Integer, Map<String, List<String>>>> byPart = new TreeMap<>();

        for (final Path page : partPages()) {
            final String text = read(page);

            for (final Pattern form : List.of(DEFINITION, IN_A_TABLE)) {
                final Matcher written = form.matcher(text);

                while (written.find()) {
                    byPart.computeIfAbsent(written.group(1), part -> new TreeMap<>())
                            .computeIfAbsent(Integer.parseInt(written.group(2)), number -> new LinkedHashMap<>())
                            .computeIfAbsent(oneLine(written.group(3)), words -> new ArrayList<>())
                            .add(page.getFileName().toString());
                }
            }
        }

        return byPart;
    }

    /**
     * A rule's words on one line, so the same rule wrapped differently on two
     * pages is one wording rather than two - and so a disagreement can be read
     * in the failure rather than diffed by hand.
     */
    private static String oneLine(final String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * The pages inside the parts, and the product page.
     * <p>
     * Nothing else at the top of {@code docs}: the standard shows the form of a
     * rule in a template, and the home page links to every part without writing
     * a rule of its own. The product page is here because its rules are real -
     * they belong to no part, which is what {@code PRODUCT} names.
     */
    private static List<Path> partPages() {
        final List<Path> pages = new ArrayList<>();

        try (Stream<Path> tree = Files.walk(DOCS)) {
            for (final Path file : tree.toList()) {
                if (!file.toString().endsWith(".md")) continue;
                if (file.getParent().equals(DOCS) && !file.getFileName().toString().equals(PRODUCT)) continue;

                pages.add(file);
            }
        } catch (final IOException ex) {
            fail("Could not read " + DOCS + ": " + ex.getMessage());
        }

        return pages;
    }

    private static List<Path> javaFiles() {
        final List<Path> files = new ArrayList<>();

        try (Stream<Path> tree = Files.walk(SOURCES)) {
            files.addAll(tree.filter(file -> file.toString().endsWith(".java")).toList());
        } catch (final IOException ex) {
            fail("Could not read " + SOURCES + ": " + ex.getMessage());
        }

        return files;
    }

    /**
     * The page carrying a part's Numbering row, from the prefix its rules carry:
     * {@code TREE-PANEL} is {@code docs/treePanel/main.md}. The product has no
     * folder, so its own page answers for it.
     */
    private static Path numberingPage(final String part) {
        if ("PRODUCT".equals(part)) return DOCS.resolve(PRODUCT);

        return partFolder(part).resolve("main.md");
    }

    /**
     * Where a part's pages live, from the prefix its rules carry:
     * {@code TREE-PANEL} is {@code docs/treePanel}.
     */
    private static Path partFolder(final String part) {
        final Map<String, String> folders = new LinkedHashMap<>();
        folders.put("TREE-PANEL", "treePanel");
        folders.put("EDITOR-PANEL", "editorPanel");
        folders.put("VIEW-PANEL", "viewPanel");
        folders.put("SETTING", "setting");
        folders.put("CODEGEN", "codegen");
        folders.put("REPORT", "report");
        folders.put("SHARE", "share");
        folders.put("INTERNAL", "internal");

        final String folder = folders.get(part);
        if (folder == null) fail("Rule-" + part + "-… names a part with no folder under docs");

        return DOCS.resolve(folder);
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
