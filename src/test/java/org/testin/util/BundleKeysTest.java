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

package org.testin.util;

import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Every key the code asks for exists, and every key the bundle holds is asked
 * for.
 * <p>
 * The plugin is being translated, so a string a tester reads is a key now rather
 * than a literal (#11). A key is a string at bottom, and the two failures it
 * brings are both silent: ask for one that is not there and the tester reads
 * {@code !done.copied!} in a balloon; leave one nothing asks for and a
 * translator is paid to translate a sentence that is never shown.
 * <p>
 * Neither is a compile error and neither shows up in a sandbox unless the exact
 * screen is opened, which is why it is checked here.
 * <p>
 * <b>The vocabulary enums are resolved as well as counted.</b> They call
 * {@code Bundle.message} in a constant's constructor, so the lookup happens
 * while the class initializes - a missing key there is an
 * {@code ExceptionInInitializerError} in front of a tester rather than a wrong
 * word, and every action that notifies anything goes through them.
 */
public class BundleKeysTest {

    /**
     * Every {@code Bundle.message("...")} written in the plugin.
     */
    private static List<String> keysAskedForInCode() {
        final List<String> keys = new ArrayList<>();
        final Pattern call = Pattern.compile("Bundle\\.message\\(\\s*\"([^\"]+)\"");

        for (final Path java : sources()) {
            final Matcher m = call.matcher(read(java));
            while (m.find()) {
                keys.add(m.group(1));
            }
        }
        return keys;
    }

    private static List<Path> sources() {
        try (var walk = Files.walk(Path.of("src", "main", "java"))) {
            return walk.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (final IOException e) {
            throw new AssertionError("Could not walk the sources", e);
        }
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new AssertionError("Could not read " + path, e);
        }
    }

    private static Properties bundle(final String name) {
        final Properties properties = new Properties();
        final Path path = Path.of("src", "main", "resources", name);

        assertTrue(Files.exists(path), path + " is missing, so nothing is translated into it");

        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (final IOException e) {
            throw new AssertionError("Could not read " + path, e);
        }
        return properties;
    }

    @Test
    public void everyKeyTheCodeAsksForIsInTheBundle() {
        final Properties english = bundle("messages.properties");

        for (final String key : keysAskedForInCode()) {
            assertTrue(english.containsKey(key),
                    "Bundle.message(\"" + key + "\") is written in the code and messages.properties has no such key,"
                            + " so the tester reads the key itself where the sentence should be");
        }
    }

    @Test
    public void everyKeyInTheBundleIsAskedForSomewhere() {
        final List<String> asked = keysAskedForInCode();
        final Properties english = bundle("messages.properties");

        for (final String key : english.stringPropertyNames()) {
            // The platform reads these itself, by name, from plugin.xml.
            if (key.startsWith("toolwindow.") || key.startsWith("action.") || key.startsWith("group.")
                    || key.equals("testin.display.name")) {
                continue;
            }

            assertTrue(asked.contains(key),
                    key + " is in messages.properties and nothing asks for it, so it is a sentence"
                            + " somebody translates and no tester ever reads");
        }
    }

    /**
     * The words every notification is built from, resolved rather than counted -
     * these are looked up while the enum initializes.
     */
    @Test
    public void theNotificationVocabularyResolves() {
        for (final Done done : Done.values()) {
            assertResolved(done.getOutcome(), "Done." + done.name());
        }

        for (final Refused refused : Refused.values()) {
            assertResolved(refused.getSentence(), "Refused." + refused.name());
        }
    }

    private static void assertResolved(final String value, final String what) {
        assertFalse(value.isBlank(), what + " resolved to nothing at all");
        assertFalse(value.startsWith("!") && value.endsWith("!"),
                what + " resolved to " + value + ", which is what the platform returns for a key it cannot find");
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-066.
     * <p>
     * Every declared action is named in the bundle, because plugin.xml no longer
     * names it.
     * <p>
     * The descriptor used to carry {@code text} and {@code description} on each
     * element; the platform reads them from here instead now, by id, so that a
     * tester running the IDE in another language sees that language in Find
     * Action and in Settings -> Keymap. A key that is missing does not fail
     * anything - the platform falls
     * back to the id, so the tester finds an action called
     * {@code Testin.RunTests} and nothing says why.
     */
    @Test
    public void everyDeclaredActionIsNamedInTheBundle() {
        final Properties english = bundle("messages.properties");
        final String xml = read(Path.of("src", "main", "resources", "META-INF", "plugin.xml"));

        final String actions = xml.substring(xml.indexOf("<actions>"), xml.indexOf("</actions>"));
        final Matcher element = Pattern.compile("<(action|group)\\b[^>]*?id=\"([^\"]+)\"").matcher(actions);

        int found = 0;
        while (element.find()) {
            final String key = element.group(1) + "." + element.group(2) + ".text";

            assertTrue(english.containsKey(key),
                    element.group(2) + " is declared in plugin.xml and " + key + " is not in the bundle,"
                            + " so Find Action and the Keymap page show the id instead of a name");
            found++;
        }

        assertTrue(found > 0, "no declared actions were found, so this test is checking nothing");
    }

    /**
     * A translation answers the same keys as the English, or a tester running in
     * that language reads a mixture of the two.
     */
    @Test
    public void everyTranslationAnswersTheSameKeys() {
        final Properties english = bundle("messages.properties");

        for (final String language : List.of("fr", "hi")) {
            final Properties other = bundle("messages_" + language + ".properties");

            assertEquals(other.stringPropertyNames(), english.stringPropertyNames(),
                    "messages_" + language + ".properties does not answer the same keys as the English bundle,"
                            + " so a tester in that language reads some of each");
        }
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-066.
     * <p>
     * A sentence with a slot in it has its apostrophes doubled, in every
     * language.
     * <p>
     * {@code Bundle.message} runs MessageFormat only when arguments are passed -
     * without them the value comes back exactly as written. So a key carrying
     * {@code {0}} is a MessageFormat pattern, and in one of those a lone
     * apostrophe is the quoting character: it is eaten, and it takes the text
     * after it with it. "l'exécution est arrêtée" prints as "lexécution est
     * arrêtée" and "{0}" inside a quoted run prints as the literal braces.
     * <p>
     * Nothing fails when this is wrong. The sentence is simply missing a letter,
     * in one language, on one screen - which is why it is checked here rather
     * than left to be noticed.
     * <p>
     * A key with {@code %s} and no {@code {0}} is not a MessageFormat pattern -
     * {@link org.testin.notifications.Refused} formats those itself - so an
     * apostrophe in one of those is written once and left alone.
     */
    @Test
    public void everyPatternWithASlotDoublesItsApostrophes() {
        final Pattern slot = Pattern.compile("\\{\\d");

        for (final String name : List.of("messages.properties", "messages_fr.properties", "messages_hi.properties")) {
            final Properties properties = bundle(name);

            for (final String key : properties.stringPropertyNames()) {
                final String value = properties.getProperty(key);
                if (!slot.matcher(value).find()) continue;

                assertFalse(value.replace("''", "").contains("'"),
                        name + " has " + key + "=" + value + ", which carries a {0} and so is read as a"
                                + " MessageFormat pattern - a lone apostrophe there is swallowed along with"
                                + " whatever follows it. Write it as ''.");
            }
        }
    }
}
