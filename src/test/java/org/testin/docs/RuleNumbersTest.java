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

public class RuleNumbersTest {

    private static final Path DOCS = Paths.get("docs");

    private static final String PRODUCT = "product.md";
    private static final Path SOURCES = Paths.get("src", "main", "java");

    private static final Pattern DEFINITION = Pattern.compile(
            "^\\s*-\\s+\\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\*(.*?)(?=\\r?\\n\\s*-\\s+\\*\\*Rule-|\\r?\\n\\r?\\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern IN_A_TABLE = Pattern.compile(
            "^\\| \\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\* \\|(.*?)\\|\\s*$", Pattern.MULTILINE);

    private static final Pattern REFERENCE = Pattern.compile("Rule-([A-Z][A-Z-]*)-(\\d+)");

    private static final Pattern RANGE = Pattern.compile("Rules are `Rule-([A-Z][A-Z-]*)-\\d+` to `Rule-[A-Z][A-Z-]*-(\\d+)`");

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

    private static String oneLine(final String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

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

    private static Path numberingPage(final String part) {
        if ("PRODUCT".equals(part)) return DOCS.resolve(PRODUCT);

        return partFolder(part).resolve("main.md");
    }

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
}
