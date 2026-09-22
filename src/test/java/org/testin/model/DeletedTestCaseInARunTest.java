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
import org.testin.model.dto.TestRunDto;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DeletedTestCaseInARunTest {

    private static TestRunItems removedItem(final UUID id) {
        return TestRunItems.builder()
                .id(id)
                .status(TestStatus.REMOVED)
                .executedAt(Config.NOT_EXECUTED)
                .build();
    }

    @Test
    public void aRowWhoseTestCaseIsGoneSaysSoByName() {
        final UUID id = UUID.randomUUID();

        assertTrue(removedItem(id).isRemoved(),
                "the verdict path, the details editor and the walker all ask this");
    }

    @Test
    public void aRemovedRowIsNotAVerdict() {
        assertFalse(TestStatus.REMOVED.isVerdict(), "nobody chose REMOVED from the menu");
    }

    @Test
    public void theRowStillShowsAndStillNamesItsTestCase() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = removedItem(id);

        final TestCaseDto shown = item.shownTestCase();

        assertEquals(shown.getId(), id, "the id is the only identity a deleted case has left");
        assertTrue(shown.getDescription().contains(id.toString()),
                "the row names itself rather than drawing blank: " + shown.getDescription());
    }

    @Test
    public void aRowNeverJudgedOfADeletedTestCaseIsShownRemoved() {
        final TestRunItems item = TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(TestStatus.PENDING)
                .removed(true)
                .build();

        assertEquals(item.shownStatus(), TestStatus.REMOVED, "nothing was executed, and now nothing can be");
    }

    @Test
    public void aJudgedRowOfADeletedTestCaseShowsItsVerdictAndIsWrittenWithIt() {
        final TestRunItems item = TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(TestStatus.PASSED)
                .removed(true)
                .build();

        assertTrue(item.isRemoved(), "the verdict path, the details editor and the walker refuse it");
        assertEquals(item.shownStatus(), TestStatus.PASSED, "a recorded result is shown whatever happened to the test case (#306)");

        try {
            final String written = new String(RealMapper.build().writeValueAsBytes(TestRunDto.builder().results(List.of(item)).build()), StandardCharsets.UTF_8);

            assertTrue(written.contains("\"PASSED\""), "the file keeps the verdict: " + written);
            assertFalse(written.contains("removed"), "the mark is never written: " + written);
        } catch (final Exception e) {
            throw new AssertionError("the run could not be written", e);
        }
    }
}
