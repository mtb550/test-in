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

import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * A run item's screenshots on disk (#50): beside the stacktrace, each as base64,
 * and no key at all when there are none - so a run with no screenshot writes the
 * file it wrote before screenshots had a place of their own.
 */
public class RunItemScreenshotsJsonTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G'};

    @Test
    public void screenshotsSurviveAWriteAndARead() {
        final TestRunItems written = TestRunItems.builder().id(UUID.randomUUID()).stacktrace("boom").screenshots(List.of(PNG_SIGNATURE)).build();

        try {
            final String json = new String(RealMapper.build().writeValueAsBytes(written), StandardCharsets.UTF_8);
            final TestRunItems read = RealMapper.build().readValue(json, TestRunItems.class);

            assertTrue(json.contains("\"iVBORw==\""), "each screenshot is written as base64: " + json);
            assertEquals(read.getStacktrace(), "boom", "the text stays text");
            assertEquals(read.getScreenshots().size(), 1);
            assertEquals(read.getScreenshots().get(0), PNG_SIGNATURE);
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
}
