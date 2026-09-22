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

package org.testin.testrun;

import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;

public class NextRunNameTest {

    @Test
    public void aTrailingNumberIsIncremented() {
        assertEquals(NextRunName.after("cycle-1", Set.of()), "cycle-2");
        assertEquals(NextRunName.after("cycle 9", Set.of()), "cycle 10");
        assertEquals(NextRunName.after("2", Set.of()), "3");
    }

    @Test
    public void aPaddedNumberIsStillOneNumber() {
        assertEquals(NextRunName.after("run09", Set.of()), "run10");
    }

    @Test
    public void aNameWithNoNumberGetsOne() {
        assertEquals(NextRunName.after("smoke", Set.of()), "smoke-2");
    }

    @Test
    public void aTakenNameIsCountedPast() {
        assertEquals(NextRunName.after("cycle-1", Set.of("cycle-2", "cycle-3")), "cycle-4");
        assertEquals(NextRunName.after("smoke", Set.of("smoke-2")), "smoke-3");
    }

    @Test
    public void aNumberTooLongToParseWholeStillCountsUp() {
        assertEquals(NextRunName.after("build-12345678901234567890", Set.of()), "build-12345678901234567891");
    }
}
