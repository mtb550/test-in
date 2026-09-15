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

package org.testin.sftp;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * What a sync remembers of a screenshot (#313): its hash, so the remembered
 * entry is the file's own and a screenshot removed on one side is a removal,
 * not a question asked at every sync.
 */
public class BaselineScreenshotTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    @Test
    public void aScreenshotIsRememberedAsTheFileItIs() {
        final String path = "Test Runs/cycle 38/k3f9a.png";
        final Baseline baseline = new Baseline(Map.of(path, Baseline.remembered(path, PNG)));

        assertEquals(baseline.manifest().entries().get(path), Manifest.Entry.of(PNG));
    }

    @Test
    public void rememberedAsTextItWouldNeverMatch() {
        final String path = "Test Runs/cycle 38/k3f9a.png";
        final Baseline asText = new Baseline(Map.of(path, new String(PNG, StandardCharsets.UTF_8)));

        assertNotEquals(asText.manifest().entries().get(path), Manifest.Entry.of(PNG),
                "the reason screenshots are remembered by hash: a PNG is not text");
    }

    @Test
    public void aTestCaseIsStillRememberedAsItsTextForMerging() {
        final String path = "Test Cases/ts2/07f7e754-b849-4b38-9e6e-a2cacd84e927.json";
        final byte[] json = "{\"description\":\"Log in\"}".getBytes(StandardCharsets.UTF_8);

        assertEquals(Baseline.remembered(path, json), "{\"description\":\"Log in\"}");
        assertEquals(new Baseline(Map.of(path, Baseline.remembered(path, json))).manifest().entries().get(path), Manifest.Entry.of(json));
    }
}
