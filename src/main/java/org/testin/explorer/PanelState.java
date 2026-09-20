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

import org.jetbrains.annotations.NotNull;

/**
 * What the explorer panel shows, decided in one place (#8).
 * <p>
 * There are six of these and the order they are tested in is the whole
 * behavior: a repository that names a test project it does not have must be
 * offered the clone, not "create your first test project", even though both
 * facts are true at once. The decision is kept out of the Swing method that
 * draws it so the order can be pinned by tests rather than by reading.
 * <p>
 * <b>Every one of them owes the tester a way forward</b>, and that is what this
 * list is for: six rows is the whole of what a first run can see, so a state
 * with no way off it is visible here rather than found by a tester (#301, D9).
 * {@code READING} is the one exception and not an exception at all - it leaves
 * by itself when indexing ends.
 */
public enum PanelState {

    /**
     * No Testin root is configured, so there is nowhere to look.
     */
    NO_ROOT,

    /**
     * Rule-TREE-PANEL-118.
     * <p>
     * The first index has not finished, so no name can resolve yet.
     * <p>
     * Before this state the panel drew the choose screen while the index was
     * being built, and a repository that names a project sitting right there
     * under the root got that name in red with <i>could not be read</i> beside
     * it - true of the index for another second and not true of anything the
     * tester could act on. Nothing to offer here, because the only thing a
     * tester could do about it is wait.
     */
    READING,

    /**
     * The repository names a test project that is not on this machine, and says
     * where it comes from. The state the committed config file exists for.
     */
    CLONE_BOUND,

    /**
     * A root with no test projects under it at all.
     */
    NO_PROJECTS,

    /**
     * Projects exist and this repository is not bound to a usable one - never
     * bound, or bound to a name nobody uses.
     */
    CHOOSE,

    /**
     * A bound project that resolved. The tree.
     */
    TREE;

    /**
     * UC-TREE-PANEL-001.
     * <p>
     * The state these facts add up to.
     *
     * @param rootConfigured       a Testin root is set
     * @param indexed              the first index has finished
     * @param projectResolved      the bound project was found in the index
     * @param boundProjectMissing  the repository names a project that is nowhere under the root
     * @param cloneUrlKnown        the config says where the test project is cloned from
     * @param anyProjectsUnderRoot at least one test project folder exists under the root
     */
    public static @NotNull PanelState of(final boolean rootConfigured, final boolean indexed, final boolean projectResolved, final boolean boundProjectMissing, final boolean cloneUrlKnown, final boolean anyProjectsUnderRoot) {
        if (!rootConfigured) return NO_ROOT;

        // The tree wins over waiting: a project that resolved is indexed by
        // definition, so there is nothing left to wait for that the tester is
        // looking at.
        if (projectResolved) return TREE;

        // Everything below is about a name that did not resolve, and until the
        // index is built that says nothing about the name (Rule-TREE-PANEL-118).
        if (!indexed) return READING;

        if (boundProjectMissing && cloneUrlKnown) return CLONE_BOUND;
        if (!anyProjectsUnderRoot) return NO_PROJECTS;

        return CHOOSE;
    }
}
