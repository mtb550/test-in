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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * What reading a test project costs, per test case, as a number (#125).
 * <p>
 * The indexer is the heart of the plugin and had no stated budget, so a change
 * that doubled the cost of a scan would ship and be discovered as "the tree
 * takes a while now". A number nobody wrote down is not a budget. The number,
 * and what it does and does not cover, is in
 * {@code docs/internal/readTestProject.md}.
 * <p>
 * <b>Two costs, and only one of them is the plugin's.</b> Measuring the whole
 * scan on this machine gave 150 seconds cold and 3.5 seconds warm for ten
 * thousand cases - a forty-fold spread on identical work, because a cold read of
 * ten thousand freshly written files on Windows is the virus scanner's number,
 * not Jackson's. Asserting on that would fail on a loaded runner and pass
 * through a real regression on a fast disk.
 * <p>
 * So the budget is on <b>parsing</b>, which is the part a commit can change,
 * measured from documents already in memory. The disk half is measured too, and
 * reported rather than asserted, because it is worth knowing and worth nobody
 * pretending it is a property of this code.
 * <p>
 * <b>Not {@code scanSingleProject}.</b> That is on a project service and this
 * repository has no platform test harness (#107) - writing the first one is a
 * larger job than the budget it would check. The progress reporting, the cache
 * writes and the VFS refresh around the parse are not measured here, and are not
 * what grows with ten thousand cases.
 */
public class IndexerBudgetTest {

    /**
     * The reference size the budget is stated at. Ten thousand is the number
     * #125 asked for, and it parses in well under a second from memory, so it
     * runs on every build rather than behind a flag.
     */
    private static final int CASES = 10_000;

    /**
     * Twice the measured cost on an ordinary machine, and deliberately no more:
     * a change that doubles the real cost has to fail this, or the budget is
     * decoration. Two is enough headroom because the parse touches no disk and
     * no network - a loaded runner makes it slower, not erratic.
     */
    private static final double BUDGET_MICROS_PER_CASE = 40.0;

    /**
     * How many timed passes to take the fastest of. Five costs a second and
     * turns a measurement that swung three-fold with the rest of the suite into
     * one that does not move.
     */
    private static final int PASSES = 5;

    /**
     * Read the way the indexer reads it, so a format change fails here too
     * rather than only in the sandbox.
     */
    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * The budget. Ten thousand documents already in memory, parsed into the DTO
     * the plugin actually holds.
     */
    @Test
    public void parsingTenThousandTestCasesStaysInsideTheBudget() {
        final @NotNull List<String> documents = documents(CASES);

        // Jackson builds a deserializer for TestCaseDto on first use, and the
        // JIT has seen nothing yet. Timing that would measure the first hundred
        // cases of a tester's first project and call it the cost of every case.
        parse(documents.subList(0, 1_000));

        long fastest = Long.MAX_VALUE;
        long slowest = 0;

        // The best of several passes, not one pass and not their average.
        // Measured alone this parse takes 21 microseconds a case; measured
        // inside the full suite, with six hundred other tests' garbage in the
        // heap, one pass reported 67. Nothing about the parse changed - the
        // collector ran during it. The fastest pass is the one least
        // interrupted, which is the number that answers "did this get slower".
        for (int pass = 0; pass < PASSES; pass++) {
            final long started = System.nanoTime();
            final @NotNull List<TestCaseDto> parsed = parse(documents);
            final long elapsed = System.nanoTime() - started;

            assertEquals(parsed.size(), CASES, "The parse read a different number of cases than it was given");

            fastest = Math.min(fastest, elapsed);
            slowest = Math.max(slowest, elapsed);
        }

        final double micros = fastest / 1_000.0 / CASES;

        System.out.printf(
                "Indexer budget: parsed %,d test cases in %,.0f ms (%.1f us/case), slowest of %d passes %,.0f ms, holding %,d KB%n",
                CASES, fastest / 1e6, micros, PASSES, slowest / 1e6, heldKilobytes(documents));

        assertTrue(micros < BUDGET_MICROS_PER_CASE,
                "Parsing a test case costs " + String.format("%.1f", micros) + " us, over the "
                        + BUDGET_MICROS_PER_CASE + " us budget in docs/internal/readTestProject.md."
                        + " Either the read got slower or the budget needs re-measuring - decide which,"
                        + " and if it is the budget, say why in that document.");
    }

    /**
     * The disk half, reported and never asserted on. Behind a flag because
     * writing and then cold-reading ten thousand files takes minutes on a
     * machine with a virus scanner, and a build should not pay that:
     * <pre>./gradlew :test --tests "*IndexerBudgetTest*" "-Dtestin.budget.cases=10000"</pre>
     */
    @Test
    public void walkingATestProjectOnDiskIsMeasuredAndReported() {
        // Outside the try: a skip is a RuntimeException, and a broad catch would
        // turn it into a failure on every machine that legitimately skips it.
        final int cases = Integer.getInteger("testin.budget.cases", 0);
        if (cases == 0) throw new SkipException("Set -Dtestin.budget.cases to measure the on-disk walk");

        final @NotNull Path root = SyntheticTree.tempRoot();

        try {
            final @NotNull Path project = SyntheticTree.write(root, Math.max(1, cases / 100), Math.min(cases, 100));

            final long cold = timeWalk(project, cases);
            final long warm = timeWalk(project, cases);

            System.out.printf("Indexer budget: %,d cases on disk · cold %,.0f ms (%.1f us/case) · warm %,.0f ms (%.1f us/case)%n",
                    cases, cold / 1e6, cold / 1_000.0 / cases, warm / 1e6, warm / 1_000.0 / cases);
        } finally {
            SyntheticTree.delete(root);
        }
    }

    /**
     * What ten thousand test cases cost to hold, to the nearest kilobyte.
     * <p>
     * A second parse rather than a reading around the timed one: asking for a
     * collection immediately before the clock starts leaves the next
     * allocations paying for it, which reported 64 microseconds a case for work
     * that takes 18. Weighing and timing are separate passes for that reason.
     * <p>
     * Reported and never asserted on - a heap reading is a snapshot of a
     * collector's opinion, and failing a build on one would be failing it on
     * the weather.
     */
    private static long heldKilobytes(final @NotNull List<String> documents) {
        final long before = usedBytes();
        final @NotNull List<TestCaseDto> weighed = parse(documents);
        final long after = usedBytes();

        assertEquals(weighed.size(), CASES, "The cases were collected before they could be weighed");
        return Math.max(0, after - before) / 1024;
    }

    private static long usedBytes() {
        final @NotNull Runtime runtime = Runtime.getRuntime();

        System.gc();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long timeWalk(final @NotNull Path project, final int expected) {
        final long started = System.nanoTime();
        final int read = readAll(project).size();
        final long elapsed = System.nanoTime() - started;

        assertEquals(read, expected, "The walk read the wrong number of cases, so the timing is of the wrong thing");
        return elapsed;
    }

    private static @NotNull List<TestCaseDto> readAll(final @NotNull Path project) {
        final @NotNull List<TestCaseDto> cases = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(project)) {
            for (final Path file : walk.toList()) {
                if (!file.getFileName().toString().endsWith(".json")) continue;

                cases.add(MAPPER.readValue(file.toFile(), TestCaseDto.class));
            }
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the generated tree at " + project + ": " + ex.getMessage(), ex);
        }

        return cases;
    }

    private static @NotNull List<TestCaseDto> parse(final @NotNull List<String> documents) {
        final @NotNull List<TestCaseDto> cases = new ArrayList<>(documents.size());

        for (final String document : documents) {
            try {
                cases.add(MAPPER.readValue(document, TestCaseDto.class));
            } catch (final IOException ex) {
                throw new AssertionError("A generated test case no longer parses: " + ex.getMessage(), ex);
            }
        }

        return cases;
    }

    private static @NotNull List<String> documents(final int count) {
        final @NotNull List<String> documents = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            documents.add(SyntheticTree.testCase(UUID.randomUUID(), "a" + i));
        }

        return documents;
    }
}
