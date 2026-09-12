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

package org.testin.indexer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A test project on disk, at whatever size the caller asks for.
 * <p>
 * Written by hand rather than through the indexer, on purpose: the indexer is a
 * project service and this repository has no platform test harness (#107), so a
 * generator that went through it could not run here at all. What it writes is
 * the format {@code docs/formats.md} specifies - the markers, the folder names,
 * and one {@code <uuid>.json} per case - which is the same thing a scan reads.
 * <p>
 * Small on purpose. It exists so a budget can be measured against a real number
 * of real files instead of estimated, and nothing else should grow onto it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SyntheticTree {

    /** Every case is the same size; what is being measured is how many there are. */
    private static final @NotNull String CASE = """
            {
              "order" : "%s",
              "id" : "%s",
              "description" : "Log in with a valid user and reach the dashboard",
              "expectedResult" : "The dashboard opens and the user name is shown",
              "status" : "REVIEWED",
              "steps" : [ "Open the login page", "Enter a valid user", "Submit" ],
              "priority" : "HIGH",
              "reference" : "JIRA-1234",
              "group" : [ "SMOKE" ],
              "createdBy" : "Sara Al-Otaibi",
              "updatedBy" : "Sara Al-Otaibi",
              "createdAt" : "Wednesday 02-09-2026 At 23:29:28 [Asia/Riyadh]",
              "updatedAt" : "Monday 07-09-2026 At 06:49:39 [Asia/Riyadh]",
              "module" : "Authentication",
              "testData" : "user=valid.user password=****",
              "preConditions" : "The user exists and is not locked"
            }""";

    private static final @NotNull String MARKER = """
            {
              "createdBy" : "Sara Al-Otaibi",
              "createdAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
              "modifiedBy" : "Sara Al-Otaibi",
              "modifiedAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
              "status" : "ACTIVE"
            }""";

    /**
     * One test case document, the same shape the plugin writes. Public to the
     * package so the budget can parse it without going anywhere near a disk.
     */
    static @NotNull String testCase(final @NotNull UUID id, final @NotNull String order) {
        return CASE.formatted(order, id);
    }

    /**
     * Writes one test project holding {@code sets} test sets of {@code perSet}
     * test cases each, and answers the folder it wrote it into.
     */
    static @NotNull Path write(final @NotNull Path root, final int sets, final int perSet) {
        final @NotNull Path project = root.resolve("BENCHMARK");
        write(project.resolve(".tp"), MARKER);
        write(project.resolve("Test Cases").resolve(".tcd"), MARKER);
        write(project.resolve("Test Runs").resolve(".trd"), MARKER);

        for (int s = 0; s < sets; s++) {
            final @NotNull Path set = project.resolve("Test Cases").resolve("set-" + s);
            write(set.resolve(".ts"), MARKER);

            for (int c = 0; c < perSet; c++) {
                final @NotNull UUID id = UUID.randomUUID();
                write(set.resolve(id + ".json"), testCase(id, "a" + c));
            }
        }

        return project;
    }

    static @NotNull Path tempRoot() {
        try {
            return Files.createTempDirectory("testin-budget");
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a temporary directory to measure in: " + ex.getMessage(), ex);
        }
    }

    static void delete(final @NotNull Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (final IOException ex) {
            // Litter, not a failed measurement - and reporting it as one would
            // hide whichever assertion actually failed.
            System.err.println("Could not clean up " + root + ": " + ex.getMessage());
        }
    }

    private static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }
}
