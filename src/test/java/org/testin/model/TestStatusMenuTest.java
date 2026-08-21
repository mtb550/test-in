package org.testin.model;

import org.testng.annotations.Test;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

/**
 * Which statuses a tester may choose, and which the plugin sets for itself.
 * <p>
 * A tester gives one of three verdicts — passed, failed or blocked. The other
 * two are the plugin's: PENDING means queued for a run that has not reached the
 * case yet, and UNTESTED means the run finished without reaching it. Neither is
 * something to pick from a menu, and the menu is built from the entries that
 * are present, so an absent entry is what keeps them off it.
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
                .filter(status -> status.getMenuEntry().isPresent())
                .toList();

        assertEquals(onMenu, List.of(TestStatus.PASSED, TestStatus.FAILED, TestStatus.BLOCKED));
    }

    @Test
    public void thePluginSetsPendingAndUntestedItself() {
        assertTrue(TestStatus.PENDING.getMenuEntry().isEmpty(), "queued for a run, not a verdict");
        assertTrue(TestStatus.UNTESTED.getMenuEntry().isEmpty(),
                "set when a run finishes without reaching the case");
    }

    @Test
    public void everyOfferedStatusHasAKeyAndAnIcon() {
        for (final TestStatus status : TestStatus.values()) {
            status.getMenuEntry().ifPresent(entry -> {
                assertNotNull(entry.icon(), status + " is offered, so it needs an icon");
                assertNotNull(entry.shortcut(), status + " is offered, so it needs a key");
            });
        }
    }

    /**
     * isVerdict answers "did a tester choose this?" out of the same menu entry
     * this class pins, and something now depends on the two meaning the same
     * thing: reading an old run drops the execution stamp from every case
     * without a verdict. Give a tester-settable status no menu entry and that
     * would start erasing real times, with nothing else failing to say so.
     */
    @Test
    public void aVerdictIsExactlyAStatusTheMenuOffers() {
        for (final TestStatus status : TestStatus.values()) {
            assertEquals(status.isVerdict(), status.getMenuEntry().isPresent(), status + " disagrees with the menu");
        }
    }

    /**
     * Two statuses sharing a key would make one of them unreachable from the
     * keyboard, and nothing else would notice.
     */
    @Test
    public void noTwoOfferedStatusesShareAKey() {
        final List<KeyStroke> keys = Arrays.stream(TestStatus.values())
                .map(TestStatus::getMenuEntry)
                .flatMap(Optional::stream)
                .map(TestStatus.MenuEntry::shortcut)
                .toList();

        assertEquals(keys.size(), keys.stream().distinct().count(), "duplicate verdict key");
    }
}
