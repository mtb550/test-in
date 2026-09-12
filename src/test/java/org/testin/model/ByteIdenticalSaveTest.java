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

package org.testin.model;

import org.testin.model.dto.TestCaseDto;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/**
 * UC-INTERNAL-002, Rule-INTERNAL-070.
 * <p>
 * The rule this project states hardest, held to a file: <b>the stored JSON is
 * byte-identical to what the tester typed.</b> Rendering may reformat a value;
 * saving never does.
 * <p>
 * A golden file rather than a set of assertions about fields, because the rule
 * is about the <em>bytes</em>. Field order, indentation, how an empty value is
 * written, how a date is spelled, whether an enum goes out as its constant or
 * its label - every one of those is a way for a save to stop being identical,
 * and not one of them is something a per-field assertion would notice. The
 * fixture is the contract; a diff against it is the failure report.
 * <p>
 * <b>It caught one the day it was written.</b> The mapper called
 * {@code setTimeZone(TimeZone.getDefault())}, which Jackson reads as an
 * instruction to convert a {@code ZonedDateTime} into the machine's zone before
 * formatting it - so the same test case saved as {@code 23:29:28 [Asia/Riyadh]}
 * on one machine and {@code 20:29:28 [UTC]} on another. Test data is shared
 * through Git, so a colleague in another zone rewrote every timestamp in a test
 * set just by opening and saving it. See {@code Mapper} for what replaced it.
 */
public class ByteIdenticalSaveTest {

    private static final Path GOLDEN = Path.of("src", "test", "resources", "golden", "test-case.json");

    /**
     * A case with something awkward in every field a formatter would be tempted
     * to tidy.
     * <p>
     * Nothing here is decorative. A blank field must stay blank rather than
     * gaining a placeholder - that was #155, where {@code EMPTY_DESCRIPTION} was
     * stored and then read back as the case's name in every report. Quotes and
     * backslashes must be escaped and not stripped. A trailing period must
     * survive, because the details panel adds one for display and the file must
     * not. Accents must survive as themselves. And the timestamps carry two
     * different zones on purpose, so a mapper that converts either of them to
     * one zone fails here.
     */
    private static TestCaseDto typedByATester() {
        final TestCaseDto tc = new TestCaseDto();

        tc.setId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        tc.setOrder("zo");
        tc.setDescription("Log in with a valid user");
        tc.setExpectedResult("The tester reaches the dashboard.");
        tc.setSteps(List.of("Open \"/login\"", "Type C:\\Users\\test", "Press Enter"));
        tc.setStatus(TestCaseStatus.TO_BE_UPDATED);
        tc.setPriority(Priority.HIGH);
        tc.setReference("JIRA-1");
        tc.setGroup(List.of("smoke", "régression"));
        tc.setCreatedBy("Muteb");
        tc.setUpdatedBy("");
        tc.setCreatedAt(ZonedDateTime.of(2026, 9, 2, 23, 29, 28, 0, ZoneId.of("Asia/Riyadh")));
        tc.setUpdatedAt(ZonedDateTime.of(2026, 9, 10, 1, 23, 59, 0, ZoneId.of("Europe/London")));
        tc.setModule("");
        tc.setTestData("{\"user\": \"admin\"}");
        tc.setPreConditions("");

        return tc;
    }

    private static String saved() {
        return new String(RealMapper.build().writeValueAsBytes(typedByATester()), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    /**
     * The fixture, written the first time if it is not there.
     * <p>
     * A golden file nobody can regenerate is a golden file somebody deletes the
     * day it fails. Writing it makes the first run of a new fixture a review
     * rather than a transcription - and it fails on that run either way, so a
     * fixture can never be created and passed over in the same breath.
     */
    private static String golden() {
        try {
            if (!Files.exists(GOLDEN)) {
                Files.createDirectories(GOLDEN.getParent());
                Files.writeString(GOLDEN, saved(), StandardCharsets.UTF_8);

                throw new AssertionError("Wrote a new fixture at " + GOLDEN.toAbsolutePath()
                        + " - read it, check it is what a tester's file should look like, and run again.");
            }

            return Files.readString(GOLDEN, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (final IOException ex) {
            throw new AssertionError("Could not read or write the golden file: " + GOLDEN.toAbsolutePath(), ex);
        }
    }

    @Test
    public void aSavedTestCaseIsByteIdenticalToTheFixture() {
        assertEquals(saved(), golden(),
                "The bytes a test case saves as have changed. If that was deliberate, update "
                        + GOLDEN + " and say in the commit what a tester now sees differently - "
                        + "and if it was not, something reformatted a value on the way to disk.");
    }

    /**
     * And the same bytes wherever the machine happens to be.
     * <p>
     * Separate from the fixture check so that a failure says which of the two
     * went wrong: the shape changed, or the machine leaked into it.
     */
    @Test
    public void theMachineTimeZoneDoesNotReachTheFile() {
        final TimeZone was = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"));
            final String riyadh = saved();

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
            final String auckland = saved();

            assertEquals(auckland, riyadh,
                    "The same test case saved differently in two time zones, so test data shared through Git"
                            + " churns whenever a colleague elsewhere opens and saves it.");
        } finally {
            TimeZone.setDefault(was);
        }
    }

    /**
     * A blank field is written blank.
     * <p>
     * Its own test because it is the failure with the longest reach: a
     * placeholder stored once is read back as the tester's own words by the
     * card, the grid, the details panel, every report and the generated method
     * name (#155).
     */
    @Test
    public void nothingTypedIsNothingStored() {
        final String json = saved();

        assertEquals(json.contains("\"module\" : \"\""), true, "an empty module gained something: " + json);
        assertEquals(json.contains("\"preConditions\" : \"\""), true, "empty preconditions gained something");
        assertEquals(json.contains("\"updatedBy\" : \"\""), true, "an empty updatedBy gained something");
    }
}
