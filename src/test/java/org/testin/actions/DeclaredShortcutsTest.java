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

package org.testin.actions;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DeclaredShortcutsTest {

    private static final Map<String, List<String>> SHARED_ON_PURPOSE = Map.of(
            "F2", List.of("Testin.UpdateTestCase", "Testin.UpdateRunItem", "Testin.EditTestRun"));

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
                byKey.computeIfAbsent(normalize(element.group(3)), _ -> new ArrayList<>()).add(owner);
            }
        }
        return byKey;
    }

    private static String actionsSection() {
        final String xml = read();
        final int from = xml.indexOf("<actions>");
        final int to = xml.indexOf("</actions>");

        assertTrue(from >= 0 && to > from, "plugin.xml has no <actions> block, so this test is checking nothing");
        return xml.substring(from, to);
    }

    private static String normalize(final String keystroke) {
        final List<String> parts = new ArrayList<>(List.of(keystroke.replace("control", "ctrl").trim().split("\\s+")));
        final String key = parts.removeLast().toUpperCase(Locale.ROOT);

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

    @Test
    public void everyDeliberatelySharedKeyIsStillShared() {
        final Map<String, List<String>> declared = declaredKeys("$default");
        declared.putAll(declaredKeys("Mac OS X 10.5+"));

        SHARED_ON_PURPOSE.forEach((key, ids) -> assertEquals(declared.get(key), ids,
                key + " is excused as shared by " + ids + ", and that is not what plugin.xml says anymore."
                        + " Remove the entry, or say which pair shares it now."));
    }

    @Test
    public void everyMacKeyBelongsToAnActionThatHasADefaultKey() {
        final Map<String, List<String>> mac = declaredKeys("Mac OS X 10.5+");
        final List<String> withDefault = declaredKeys("$default").values().stream().flatMap(List::stream).toList();

        mac.values().stream().flatMap(List::stream).forEach(id -> assertTrue(withDefault.contains(id),
                id + " has a Mac key and no default key, so it answers on a Mac and nowhere else"));
    }
}
