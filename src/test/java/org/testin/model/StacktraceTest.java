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

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Splitting a stored stacktrace into its text and its pasted screenshots (#28).
 */
public class StacktraceTest {

    /**
     * The eight bytes every PNG opens with, then anything: the split reads the
     * signature, not the picture.
     */
    private static final byte[] PNG = concat(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'},
            "pixels".getBytes(StandardCharsets.UTF_8));

    private static byte[] concat(final byte[] first, final byte[] second) {
        final byte[] both = new byte[first.length + second.length];
        System.arraycopy(first, 0, both, 0, first.length);
        System.arraycopy(second, 0, both, first.length, second.length);
        return both;
    }

    private static String pasted() {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG);
    }

    @Test
    public void textAloneIsText() {
        final Stacktrace stacktrace = Stacktrace.of("java.lang.AssertionError: expected [11.3]\n    at Login.check(Login.java:42)");

        assertEquals(stacktrace.text(), "java.lang.AssertionError: expected [11.3]\n    at Login.check(Login.java:42)");
        assertTrue(stacktrace.screenshots().isEmpty());
        assertEquals(stacktrace.segments().size(), 1);
    }

    @Test
    public void aScreenshotAloneIsAScreenshot() {
        final Stacktrace stacktrace = Stacktrace.of(pasted());

        assertEquals(stacktrace.text(), "");
        assertEquals(stacktrace.screenshots().size(), 1);
        assertEquals(stacktrace.screenshots().getFirst(), PNG);
        assertEquals(stacktrace.firstLine(), "", "a screenshot is never the first line");
    }

    @Test
    public void textAndScreenshotsKeepTheirOrder() {
        final Stacktrace stacktrace = Stacktrace.of("before\n" + pasted() + "\nbetween\n" + pasted() + "\nafter");

        assertEquals(stacktrace.segments().size(), 5);
        assertTrue(stacktrace.segments().get(0) instanceof Stacktrace.Text);
        assertTrue(stacktrace.segments().get(1) instanceof Stacktrace.Screenshot);
        assertTrue(stacktrace.segments().get(2) instanceof Stacktrace.Text);
        assertTrue(stacktrace.segments().get(3) instanceof Stacktrace.Screenshot);
        assertEquals(stacktrace.screenshots().size(), 2);
        assertEquals(stacktrace.text(), "before\n\nbetween\n\nafter");
    }

    @Test
    public void theFirstLineIsTheFirstTextThatSaysAnything() {
        assertEquals(Stacktrace.of(pasted() + "\n\n  java.lang.AssertionError: boom  \nat x").firstLine(),
                "java.lang.AssertionError: boom", "a stacktrace that opens with a screenshot still has a first line");
    }

    @Test
    public void somethingThatIsNotAPngStaysText() {
        final String decodesToJunk = "data:image/png;base64,abc";
        final String willNotDecode = "data:image/png;base64,abcde";

        final Stacktrace stacktrace = Stacktrace.of("error\n" + decodesToJunk + "\n" + willNotDecode);

        assertTrue(stacktrace.screenshots().isEmpty(), "neither is a PNG, so neither is a screenshot");
        assertEquals(stacktrace.text(), "error\n" + decodesToJunk + "\n" + willNotDecode, "and nothing stored is dropped");
    }

    @Test
    public void nothingIsNothing() {
        assertTrue(Stacktrace.of("").segments().isEmpty());
        assertEquals(Stacktrace.of("").firstLine(), "");
    }
}
