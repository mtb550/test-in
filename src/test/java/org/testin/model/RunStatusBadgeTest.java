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

import static org.testng.Assert.*;

/**
 * A run status either draws a badge or it does not (#48).
 * <p>
 * The label and the color used to be separate fields - a {@code @NotNull} label
 * beside a {@code @Nullable} color - which let IDLE carry the label {@code ""}
 * for a badge it never draws, and made "has a label but no color" a state the
 * type allowed. They are one fact, so they are one field, and the card asks
 * {@code hasBadge()} once instead of a predicate the checker could not follow.
 */
public class RunStatusBadgeTest {

    @Test
    public void idleDrawsNoBadge() {
        assertFalse(RunStatus.IDLE.hasBadge(), "a case nobody has run carries no badge");
        assertSame(RunStatus.IDLE.getBadge(), RunStatus.Badge.NONE, "and it says so with the empty badge");
    }

    @Test
    public void everyOtherStatusDrawsOne() {
        for (final RunStatus status : RunStatus.values()) {
            if (status == RunStatus.IDLE) continue;

            assertTrue(status.hasBadge(), status + " should draw a badge");

            final RunStatus.Badge badge = status.getBadge();
            assertNotSame(badge, RunStatus.Badge.NONE, status + " needs a badge of its own");
            assertFalse(badge.label().isBlank(), status + " has a visible label");
        }
    }

    @Test
    public void theLabelsAreTheOnesTheCardShows() {
        assertEquals(RunStatus.PASSED.getBadge().label(), "Passed");
        assertEquals(RunStatus.FAILED.getBadge().label(), "Failed");
        assertEquals(RunStatus.RUNNING.getBadge().label(), "Running");
    }

    @Test
    public void everyStatusHasAnIcon() {
        for (final RunStatus status : RunStatus.values()) {
            assertNotNull(status.getIcon(), status + " needs an icon for the gutter");
        }
    }
}
