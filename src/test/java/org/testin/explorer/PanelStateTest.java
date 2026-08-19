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
     * Nothing is decided before there is a root to look in. A repository can
     * name a project, know its URL and still be told to set the root first,
     * because none of the rest can be checked without one.
     */
    @Test
    public void noRootBeatsEverything() {
        assertEquals(PanelState.of(false, true, true, true, true), PanelState.NO_ROOT);
        assertEquals(PanelState.of(false, false, false, false, false), PanelState.NO_ROOT);
    }

    /**
     * A project that resolved is the tree, whatever else is also true.
     */
    @Test
    public void aResolvedProjectIsTheTree() {
        assertEquals(PanelState.of(true, true, false, false, true), PanelState.TREE);
        assertEquals(PanelState.of(true, true, false, true, true), PanelState.TREE);
    }

    /**
     * The reason the config file is committed: this machine has the automation
     * repository and not the test data, and the file says where to get it.
     */
    @Test
    public void aMissingProjectWithAUrlIsCloned() {
        assertEquals(PanelState.of(true, false, true, true, false), PanelState.CLONE_BOUND);

        // Even with other projects sitting under the root: the tester asked for
        // this one, so offering the picker instead would answer a different
        // question than the one the repository has already answered.
        assertEquals(PanelState.of(true, false, true, true, true), PanelState.CLONE_BOUND);
    }

    /**
     * A named project that is missing and has no URL cannot be fetched, so the
     * tester picks - from an empty root that means creating the first one.
     */
    @Test
    public void aMissingProjectWithoutAUrlFallsBack() {
        assertEquals(PanelState.of(true, false, true, false, false), PanelState.NO_PROJECTS);
        assertEquals(PanelState.of(true, false, true, false, true), PanelState.CHOOSE);
    }

    /**
     * A root with projects and no binding is the first run every existing user
     * lands on after upgrading: one step, not an error.
     */
    @Test
    public void anUnboundRepositoryChooses() {
        assertEquals(PanelState.of(true, false, false, false, true), PanelState.CHOOSE);
    }

    /**
     * A project that is under the root but did not resolve - archived is the one
     * that happens - is a choice, not a clone. It is already on this machine.
     */
    @Test
    public void anArchivedProjectChooses() {
        assertEquals(PanelState.of(true, false, false, true, true), PanelState.CHOOSE);
    }

    /**
     * An empty root sends the tester to create the first project.
     */
    @Test
    public void anEmptyRootCreates() {
        assertEquals(PanelState.of(true, false, false, false, false), PanelState.NO_PROJECTS);
    }
}
