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
 * A rule number is a name, and a name has to belong to one thing.
 * <p>
 * Rules are numbered per part and a new one takes the number after that part's
 * last, so the only way the numbering can go wrong is a collision: two rules
 * given the same number because whoever wrote the second one read the wrong
 * last number. That happened on 7 September 2026, an hour after the scheme was
 * settled, and the audit that caught it was a script nobody would run again.
 * <p>
 * Written as a scan of the documents for the reason
 * {@code GridEditConfirmationTest} gives: what has to hold is a rule about the
 * shape of what is written down, and there is nothing to run it against but the
 * files themselves.
 */
public class RuleNumbersTest {

    private static final Path DOCS = Paths.get("docs");
    private static final Path SOURCES = Paths.get("src", "main", "java");

    /**
     * A rule being defined: the bullet that states it, on the page that owns it.
     * Only in a part's own folder - the standard shows the form in a template,
     * and the product page quotes numbers it does not define.
     */
    private static final Pattern DEFINITION = Pattern.compile("^\\s*-\\s+\\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\*", Pattern.MULTILINE);

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
    public void everyRuleNumberIsUsedOnce() {
        final Map<String, Map<Integer, List<String>>> byPart = definitions();

        final List<String> clashes = new ArrayList<>();
        byPart.forEach((part, numbers) -> numbers.forEach((number, pages) -> {
            if (pages.size() > 1) clashes.add(String.format("Rule-%s-%03d is defined in %s", part, number, pages));
        }));

        if (!clashes.isEmpty()) {
            fail("A rule number names one rule. These name more than one, so a marker in the code, "
                    + "an issue and a commit all point at two things at once:\n  " + String.join("\n  ", clashes));
        }
    }

    /**
     * The row a writer reads to pick the next number has to be the truth, or the
     * next rule collides with the last one.
     */
    @Test
    public void everyPartSaysItsLastNumber() {
        final Map<String, Map<Integer, List<String>>> byPart = definitions();

        for (final Map.Entry<String, Map<Integer, List<String>>> part : byPart.entrySet()) {
            final Path main = partFolder(part.getKey()).resolve("main.md");
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
        final Set<String> defined = new LinkedHashSet<>();
        definitions().forEach((part, numbers) ->
                numbers.keySet().forEach(number -> defined.add(String.format("Rule-%s-%03d", part, number))));

        final List<String> dangling = new ArrayList<>();
        for (final Path source : javaFiles()) {
            final Matcher cited = REFERENCE.matcher(read(source));

            while (cited.find()) {
                if (!defined.contains(cited.group())) dangling.add(cited.group() + " in " + source.getFileName());
            }
        }

        if (!dangling.isEmpty()) {
            fail("These markers name rules no document defines:\n  " + String.join("\n  ", dangling));
        }
    }

    /**
     * Every rule the documents define, by part and number, remembering which
     * pages defined it so a clash can name them.
     */
    private static Map<String, Map<Integer, List<String>>> definitions() {
        final Map<String, Map<Integer, List<String>>> byPart = new TreeMap<>();

        for (final Path page : partPages()) {
            final Matcher defined = DEFINITION.matcher(read(page));

            while (defined.find()) {
                byPart.computeIfAbsent(defined.group(1), part -> new TreeMap<>())
                        .computeIfAbsent(Integer.parseInt(defined.group(2)), number -> new ArrayList<>())
                        .add(page.getFileName().toString());
            }
        }

        return byPart;
    }

    /**
     * The pages inside the parts, and nothing at the top of {@code docs} - the
     * standard shows the form of a rule in a template, and the product page
     * quotes numbers that belong to other parts.
     */
    private static List<Path> partPages() {
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
