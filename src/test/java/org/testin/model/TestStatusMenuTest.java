package org.testin.model;

import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Which statuses a tester may choose, and which the plugin sets for itself.
 * <p>
 * A tester gives one of three verdicts — passed, failed or blocked. The other
 * two are the plugin's: PENDING means queued for a run that has not reached the
 * case yet, and UNTESTED means the run finished without reaching it. Neither is
 * something to pick from a menu, and the menu is built from the statuses that
 * are verdicts, so {@link TestStatus.MenuEntry#NONE} is what keeps them off it.
 * <p>
 * Asserted because the failure is silent in both directions: give one of the two
 * an entry and it appears in the context menu with a key that sets a state
 * nothing reconciles; take an entry from one of the three and the verdict simply
 * stops being offered, with nothing failing to say so.
 */
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

    /**
     * The two the plugin sets carry the entry that draws nothing, which is the
     * whole of what keeps them off the menu.
     */
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

    /**
     * Two statuses sharing a key would make one of them unreachable from the
     * keyboard, and nothing else would notice.
     */
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
