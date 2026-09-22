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

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

public class TestStatusMenuTest {

    @Test
    public void aTesterChoosesExactlyPassedFailedOrBlocked() {
        final List<TestStatus> onMenu = Arrays.stream(TestStatus.values())
                .filter(TestStatus::isVerdict)
                .toList();

        assertEquals(onMenu, List.of(TestStatus.PASSED, TestStatus.FAILED, TestStatus.BLOCKED));
    }

    @Test
    public void thePluginSetsPendingAndUntestedItself() {
        assertFalse(TestStatus.PENDING.isVerdict(), "queued for a run, not a verdict");
        assertFalse(TestStatus.UNTESTED.isVerdict(), "set when a run finishes without reaching the case");
    }

    @Test
    public void theStatusesOffMenuCarryTheEmptyEntry() {
        assertSame(TestStatus.PENDING.getMenuEntry(), TestStatus.MenuEntry.NONE);
        assertSame(TestStatus.UNTESTED.getMenuEntry(), TestStatus.MenuEntry.NONE);
    }

    @Test
    public void everyOfferedStatusHasAKeyAndAnIcon() {
        Arrays.stream(TestStatus.values())
                .filter(TestStatus::isVerdict)
                .forEach(status -> {
                    assertNotSame(status.getMenuEntry(), TestStatus.MenuEntry.NONE,
                            status + " is offered, so it needs an entry of its own");
                    assertNotEquals(status.getMenuEntry().shortcut().getKeyCode(), KeyEvent.VK_UNDEFINED,
                            status + " is offered, so it needs a key that reaches it");
                });
    }

    @Test
    public void noTwoOfferedStatusesShareAKey() {
        final List<KeyStroke> keys = Arrays.stream(TestStatus.values())
                .filter(TestStatus::isVerdict)
                .map(TestStatus::getMenuEntry)
                .map(TestStatus.MenuEntry::shortcut)
                .toList();

        assertEquals(keys.size(), keys.stream().distinct().count(), "duplicate verdict key");
    }
}
