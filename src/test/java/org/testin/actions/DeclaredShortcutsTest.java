package org.testin.actions;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * What {@code plugin.xml} declares to the keymap, checked against itself.
 * <p>
 * A keystroke claimed by two actions does not fail, and that is the problem: the
 * platform gathers every action bound to the key, runs whichever one is enabled,
 * and says nothing at all when two are. Testin lost {@code Ctrl+Shift+C} to the
 * IDE for weeks that way - the tester pressed it in a test editor and got
 * <i>Copy Path</i>, because a Testin editor is a file editor and IntelliJ has
 * owned that key since long before this plugin (#119).
 * <p>
 * The collision with the IDE is a decision rather than a defect and is not
 * checked here - the keys are in {@code docs/shortcuts.md} with what each
 * competes against. What is checked is the half nobody chose: two <b>Testin</b>
 * actions on one key, which is always a mistake and always invisible.
 */
public class DeclaredShortcutsTest {

    /**
     * Every {@code <keyboard-shortcut>} the plugin declares, as key to the ids
     * claiming it. Read from the descriptor rather than from a list kept beside
     * it, so it cannot go stale the first time somebody adds an action.
     * <p>
     * Each shortcut belongs to the nearest {@code id} above it, because that is
     * the element it is written inside. Matching {@code <action ...>...</action>}
     * as a block does not work and fails quietly: the body is non-greedy, so it
     * ends at the first {@code />} - which is the first {@code keyboard-shortcut}
     * child. Written that way, this test read every default key and no Mac key
     * at all, and reported the Mac keymap as clean.
     */
    private static Map<String, List<String>> declaredKeys(final String keymap) {
        final String actions = actionsSection();

        final Map<String, List<String>> byKey = new LinkedHashMap<>();
        final Matcher element = Pattern.compile(
                "id=\"([^\"]+)\"|<keyboard-shortcut\\s+keymap=\"([^\"]+)\"\\s+first-keystroke=\"([^\"]+)\"").matcher(actions);

        String owner = "";
        while (element.find()) {
            if (element.group(1) != null) {
                owner = element.group(1);
                continue;
            }

            if (keymap.equals(element.group(2))) {
                byKey.computeIfAbsent(normalize(element.group(3)), key -> new ArrayList<>()).add(owner);
            }
        }
        return byKey;
    }

    /**
     * The {@code <actions>} block alone. Ids live in the extension declarations
     * too, and one of those would become the owner of whatever shortcut came
     * after it.
     */
    private static String actionsSection() {
        final String xml = read();
        final int from = xml.indexOf("<actions>");
        final int to = xml.indexOf("</actions>");

        assertTrue(from >= 0 && to > from, "plugin.xml has no <actions> block, so this test is checking nothing");
        return xml.substring(from, to);
    }

    /**
     * One spelling for one key. The descriptor writes {@code ctrl} and the
     * platform keymaps write {@code control}, and modifier order is free - so
     * two entries that are the same key can look like two keys.
     */
    private static String normalize(final String keystroke) {
        final List<String> parts = new ArrayList<>(List.of(keystroke.replace("control", "ctrl").trim().split("\\s+")));
        final String key = parts.remove(parts.size() - 1).toUpperCase(Locale.ROOT);

        parts.sort(String::compareTo);
        parts.add(key);
        return String.join(" ", parts);
    }

    private static String read() {
        final Path path = Path.of("src", "main", "resources", "META-INF", "plugin.xml");
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new AssertionError("Could not read " + path.toAbsolutePath(), e);
        }
    }

    /**
     * The two keys a pair of Testin actions is meant to share, and exactly which
     * pair shares each.
     * <p>
     * Both are one command worded for the surface the tester is standing in:
     * Ctrl+M creates the obvious thing here, F2 changes the obvious thing here.
     * Each member is enabled in one surface and gray in the others, so only one
     * of them can ever answer the key.
     * <p>
     * F2 has three: Update Test Case in the test editor, Failed Test Case
     * Details in the run editor, and Edit Test Run in the tree.
     * <p>
     * <b>Pinned to the exact pair rather than allowed as a key.</b> A list that
     * said "Ctrl+M may be shared" would keep passing when a third action joined
     * it, or when one of these two was renamed away and something else took its
     * place - which is the whole failure this test exists to catch. Written as
     * the membership, so any change to it fails here and has to be meant.
     */
    private static final Map<String, List<String>> SHARED_ON_PURPOSE = Map.of(
            "ctrl M", List.of("Testin.CreateTestCase", "Testin.CreateNode"),
            "F2", List.of("Testin.UpdateTestCase", "Testin.UpdateRunItem", "Testin.EditTestRun"),
            "meta M", List.of("Testin.CreateTestCase", "Testin.CreateNode"));

    /**
     * The check itself. Two Testin actions on one key means one of them never
     * runs, and which one is decided by whichever happens to be enabled - so
     * every such key is either on the list above or a mistake.
     */
    @Test
    public void noKeyIsClaimedByTwoTestinActionsByAccident() {
        for (final String keymap : List.of("$default", "Mac OS X 10.5+")) {
            declaredKeys(keymap).forEach((key, ids) -> {
                if (ids.size() == 1) return;

                assertEquals(ids, SHARED_ON_PURPOSE.get(key),
                        keymap + " binds " + key + " to " + ids + ". The platform runs whichever of them is"
                                + " enabled and says nothing when both are, so one of these never runs and nothing"
                                + " reports it. A key two actions are meant to share belongs in SHARED_ON_PURPOSE,"
                                + " named by the exact pair that shares it.");
            });
        }
    }

    /**
     * And the list does not outlive what it excuses. An entry for a key nothing
     * shares any more is a permission nobody reads, still granting itself.
     */
    @Test
    public void everyDeliberatelySharedKeyIsStillShared() {
        final Map<String, List<String>> declared = declaredKeys("$default");
        declared.putAll(declaredKeys("Mac OS X 10.5+"));

        SHARED_ON_PURPOSE.forEach((key, ids) -> assertEquals(declared.get(key), ids,
                key + " is excused as shared by " + ids + ", and that is not what plugin.xml says any more."
                        + " Remove the entry, or say which pair shares it now."));
    }

    /**
     * The Mac keymap says the same things as the default one, or a Mac tester
     * has an action the rest do not (#25).
     */
    @Test
    public void everyMacKeyBelongsToAnActionThatHasADefaultKey() {
        final Map<String, List<String>> mac = declaredKeys("Mac OS X 10.5+");
        final List<String> withDefault = declaredKeys("$default").values().stream().flatMap(List::stream).toList();

        mac.values().stream().flatMap(List::stream).forEach(id -> assertTrue(withDefault.contains(id),
                id + " has a Mac key and no default key, so it answers on a Mac and nowhere else"));
    }
}
