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

import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * A run item's screenshots on disk (#50, #313): the run file names PNG files
 * beside it, no picture is written into it, and a run item with none writes no
 * key at all - so a run with no screenshot writes the file it always wrote.
 */
public class RunItemScreenshotsJsonTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G'};

    @Test
    public void theRunFileNamesTheScreenshotsAndHoldsNoPicture() {
        final String name = TestRunDirectoryDto.screenshotName(PNG_SIGNATURE);
        final TestRunItems written = TestRunItems.builder().id(UUID.randomUUID()).stacktrace("boom").screenshots(List.of(name)).build();

        try {
            final String json = new String(RealMapper.build().writeValueAsBytes(written), StandardCharsets.UTF_8);
            final TestRunItems read = RealMapper.build().readValue(json, TestRunItems.class);

            assertTrue(json.contains("\"" + name + "\""), "the file is named: " + json);
            assertFalse(json.contains("iVBORw"), "and no base64 of the picture is written: " + json);
            assertEquals(read.getScreenshots(), List.of(name));
            assertEquals(read.getStacktrace(), "boom", "the text stays text");
        } catch (final Exception e) {
            throw new AssertionError("the run item could not be written and read back", e);
        }
    }

    @Test
    public void aRunItemWithNoScreenshotWritesNoKey() {
        try {
            final String json = new String(RealMapper.build().writeValueAsBytes(TestRunItems.builder().id(UUID.randomUUID()).build()), StandardCharsets.UTF_8);

            assertFalse(json.contains("screenshots"), json);
        } catch (final Exception e) {
            throw new AssertionError("the run item could not be written", e);
        }
    }

    /**
     * Named by content: the same picture is the same file, a different one is
     * another, and nothing but such a name is taken for a screenshot.
     */
    @Test
    public void aScreenshotIsNamedByWhatItHolds() {
        final String name = TestRunDirectoryDto.screenshotName(PNG_SIGNATURE);

        assertTrue(name.matches("[0-9a-f]{16}\\.png"), name);
        assertEquals(TestRunDirectoryDto.screenshotName(PNG_SIGNATURE.clone()), name, "the same bytes, the same name");
        assertNotEquals(TestRunDirectoryDto.screenshotName(new byte[]{1, 2, 3}), name, "other bytes, another name");

        assertTrue(TestRunDirectoryDto.isScreenshotName(name));
        assertFalse(TestRunDirectoryDto.isScreenshotName("run.json"));
        assertFalse(TestRunDirectoryDto.isScreenshotName("my notes.png"), "a PNG put there by hand is not a screenshot");
    }
}
