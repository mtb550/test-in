package org.testin.explorer;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * The state table the explorer panel is built from (#8).
 * <p>
 * Every one of these is reachable by a tester and several are true at the same
 * time, so what is pinned here is the <b>order</b>: a repository that names a
 * test project it does not have gets offered the clone, not the "create your
 * first test project" line that is equally true of it. Reading the branches in
 * the panel cannot prove that; these can.
 */
public class PanelStateTest {


    /**
     * The six facts, named at the call so a row of booleans is readable. The
     * order is the order {@link PanelState#of} declares them in.
     */
    private static PanelState of(final boolean rootConfigured, final boolean configUnreadable, final boolean projectResolved, final boolean boundProjectMissing, final boolean cloneUrlKnown, final boolean anyProjectsUnderRoot) {
        return PanelState.of(rootConfigured, configUnreadable, projectResolved, boundProjectMissing, cloneUrlKnown, anyProjectsUnderRoot);
    }

    /**
     * UC-TREE-PANEL-001.
     * <p>
     * A file that would not parse is its own answer, and it comes before every
     * other one but the missing root.
     * <p>
     * It used to be indistinguishable from a repository nobody had bound: the
     * loader answered EMPTY for both, so a mistyped indent was reported as "not
     * bound to a test project" and then quietly bound to whatever single project
     * was under the root - writing into the broken file, on every open, and
     * never saying so (#66, finding 10).
     */
    @Test
    public void aBrokenConfigIsItsOwnAnswer() {
        assertEquals(of(true, true, false, false, false, true), PanelState.BROKEN_CONFIG,
                "a file that would not parse is why nothing is bound, so the tester is told that rather than asked to choose");

        assertEquals(of(true, true, false, true, true, true), PanelState.BROKEN_CONFIG,
                "a URL read out of a file that would not parse is not a URL to clone from");

        assertEquals(of(false, true, false, false, false, false), PanelState.NO_ROOT,
                "with no root there is nowhere to look, which is true before the file is worth reading");
    }

    /**
     * The state every other case in this file is about: a file that read
     * cleanly, whatever it did or did not say.
     */
    @Test
    public void aReadableFileLeavesTheRestOfTheTableAlone() {
        assertEquals(of(true, false, true, false, false, true), PanelState.TREE);
    }

    /**
     * Nothing is decided before there is a root to look in. A repository can
     * name a project, know its URL and still be told to set the root first,
     * because none of the rest can be checked without one.
     */
    @Test
    public void noRootBeatsEverything() {
        assertEquals(of(false, false, true, true, true, true), PanelState.NO_ROOT);
        assertEquals(of(false, false, false, false, false, false), PanelState.NO_ROOT);
    }

    /**
     * A project that resolved is the tree, whatever else is also true.
     */
    @Test
    public void aResolvedProjectIsTheTree() {
        assertEquals(of(true, false, true, false, false, true), PanelState.TREE);
        assertEquals(of(true, false, true, false, true, true), PanelState.TREE);
    }

    /**
     * The reason the config file is committed: this machine has the automation
     * repository and not the test data, and the file says where to get it.
     */
    @Test
    public void aMissingProjectWithAUrlIsCloned() {
        assertEquals(of(true, false, false, true, true, false), PanelState.CLONE_BOUND);

        // Even with other projects sitting under the root: the tester asked for
        // this one, so offering the picker instead would answer a different
        // question than the one the repository has already answered.
        assertEquals(of(true, false, false, true, true, true), PanelState.CLONE_BOUND);
    }

    /**
     * A named project that is missing and has no URL cannot be fetched, so the
     * tester picks - from an empty root that means creating the first one.
     */
    @Test
    public void aMissingProjectWithoutAUrlFallsBack() {
        assertEquals(of(true, false, false, true, false, false), PanelState.NO_PROJECTS);
        assertEquals(of(true, false, false, true, false, true), PanelState.CHOOSE);
    }

    /**
     * A root with projects and no binding is the first run every existing user
     * lands on after upgrading: one step, not an error.
     */
    @Test
    public void anUnboundRepositoryChooses() {
        assertEquals(of(true, false, false, false, false, true), PanelState.CHOOSE);
    }

    /**
     * A project that is under the root but did not resolve - archived is the one
     * that happens - is a choice, not a clone. It is already on this machine.
     */
    @Test
    public void anArchivedProjectChooses() {
        assertEquals(of(true, false, false, false, true, true), PanelState.CHOOSE);
    }

    /**
     * An empty root sends the tester to create the first project.
     */
    @Test
    public void anEmptyRootCreates() {
        assertEquals(of(true, false, false, false, false, false), PanelState.NO_PROJECTS);
    }
}
