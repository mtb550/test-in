package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Which statuses the tree draws beside a node's name (#66, finding 7).
 * <p>
 * A node in current work says nothing: "Active" beside every test project,
 * every test set and every package is a word the tester reads a hundred times
 * and needed never. Everything else says which it is - an inactive project, a
 * deprecated test set, an archived package.
 * <p>
 * Pinned here because the renderer asks one question of three enums, and a
 * status added to any of them has to answer it. A new constant that forgot
 * would be drawn - or not drawn - by accident.
 */
public class StatusWorthShowingTest {

    @Test
    public void exactlyOneStatusPerEnumIsTheActiveOne() {
        assertActiveIs(ProjectStatus.values(), ProjectStatus.ACTIVE);
        assertActiveIs(TestSetStatus.values(), TestSetStatus.ACTIVE);
        assertActiveIs(PackageStatus.values(), PackageStatus.ACTIVE);
    }

    /**
     * The three that are worth the space, named so the test says what a tester
     * would see rather than only that the flags differ.
     */
    @Test
    public void theStatusesATesterSeesAreTheOnesThatAreNotActive() {
        assertFalse(ProjectStatus.INACTIVE.isActive(), "an inactive test project says so beside its name");
        assertFalse(ProjectStatus.ARCHIVED.isActive(), "and an archived one would, if it were ever indexed");
        assertFalse(TestSetStatus.DEPRECATED.isActive(), "a deprecated test set says so");
        assertFalse(PackageStatus.ARCHIVED.isActive(), "an archived package says so");
    }

    /**
     * The status of a node that has none answers active, so a marker carrying
     * nothing draws nothing rather than being asked about.
     */
    @Test
    public void noStatusAtAllIsNothingToSay() {
        assertTrue(NodeStatus.NONE.isActive(), "a marker with no status has nothing to draw");
        assertTrue(NodeStatus.NONE.getLabel().isEmpty(), "and nothing to draw it with");
    }

    private static void assertActiveIs(final @NotNull NodeStatus @NotNull [] values, final @NotNull NodeStatus expected) {
        final @NotNull List<NodeStatus> active = Arrays.stream(values).filter(NodeStatus::isActive).toList();

        assertTrue(active.equals(List.of(expected)),
                expected.getClass().getSimpleName() + " must have exactly one status meaning \"in current work\","
                        + " because the tree draws every other one beside the name. Active: " + active);
    }
}
