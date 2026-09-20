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
    private static PanelState of(final boolean rootConfigured, final boolean indexed, final boolean projectResolved, final boolean boundProjectMissing, final boolean cloneUrlKnown, final boolean anyProjectsUnderRoot) {
        return PanelState.of(rootConfigured, indexed, projectResolved, boundProjectMissing, cloneUrlKnown, anyProjectsUnderRoot);
    }

    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-118.
     * <p>
     * Nothing resolves before the index is built, so nothing below is decided
     * yet: every row that is not the tree waits.
     * <p>
     * A test project is found in the index, and the panel is built the moment the
     * tool window opens - which on a cold start is while the index is still being
     * made. So the name a repository was bound to resolved to nothing for a
     * second, the choose screen came up, and it carried that name in red with
     * <i>could not be read</i> beside it: the one screen a tester should never be
     * shown about a project that is sitting right there and merely not read yet.
     */
    @Test
    public void nothingIsDecidedBeforeTheIndexIsBuilt() {
        assertEquals(of(true, false, false, false, false, true), PanelState.READING,
                "a name that has not resolved because nothing is indexed yet is not a name that resolves to nothing");

        assertEquals(of(true, false, false, true, true, true), PanelState.READING,
                "the clone offer waits one draw too, rather than being made about a listing the index has not seen");

        assertEquals(of(false, false, false, false, false, false), PanelState.NO_ROOT,
                "with no root nothing indexes at all, so there is nothing to wait for - only a folder to set");
    }

    /**
     * A project that resolved is the tree even mid-index: resolving means it is
     * in the index, so there is nothing left to wait for.
     */
    @Test
    public void aResolvedProjectBeatsTheWait() {
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
        assertEquals(of(true, true, true, false, false, true), PanelState.TREE);
        assertEquals(of(true, true, true, false, true, true), PanelState.TREE);
    }

    /**
     * The reason the config file is committed: this machine has the automation
     * repository and not the test data, and the file says where to get it.
     */
    @Test
    public void aMissingProjectWithAUrlIsCloned() {
        assertEquals(of(true, true, false, true, true, false), PanelState.CLONE_BOUND);

        // Even with other projects sitting under the root: the tester asked for
        // this one, so offering the picker instead would answer a different
        // question than the one the repository has already answered.
        assertEquals(of(true, true, false, true, true, true), PanelState.CLONE_BOUND);
    }

    /**
     * A named project that is missing and has no URL cannot be fetched, so the
     * tester picks - from an empty root that means creating the first one.
     */
    @Test
    public void aMissingProjectWithoutAUrlFallsBack() {
        assertEquals(of(true, true, false, true, false, false), PanelState.NO_PROJECTS);
        assertEquals(of(true, true, false, true, false, true), PanelState.CHOOSE);
    }

    /**
     * A root with projects and no binding is the first run every existing user
     * lands on after upgrading: one step, not an error.
     */
    @Test
    public void anUnboundRepositoryChooses() {
        assertEquals(of(true, true, false, false, false, true), PanelState.CHOOSE);
    }

    /**
     * A project that is under the root but did not resolve - archived is the one
     * that happens - is a choice, not a clone. It is already on this machine.
     */
    @Test
    public void anArchivedProjectChooses() {
        assertEquals(of(true, true, false, false, true, true), PanelState.CHOOSE);
    }

    /**
     * An empty root sends the tester to create the first project.
     */
    @Test
    public void anEmptyRootCreates() {
        assertEquals(of(true, true, false, false, false, false), PanelState.NO_PROJECTS);
    }
}
