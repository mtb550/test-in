package org.testin.explorer;

import org.jetbrains.annotations.NotNull;

/**
 * What the explorer panel shows, decided in one place (#8).
 * <p>
 * There are five of these and the order they are tested in is the whole
 * behavior: a repository that names a test project it does not have must be
 * offered the clone, not "create your first test project", even though both
 * facts are true at once. The decision is kept out of the Swing method that
 * draws it so the order can be pinned by tests rather than by reading.
 */
public enum PanelState {

    /**
     * No Testin root is configured, so there is nowhere to look.
     */
    NO_ROOT,

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
     * bound, bound to a name nobody uses, or bound to an archived project.
     */
    CHOOSE,

    /**
     * A bound project that resolved. The tree.
     */
    TREE;

    /**
     * The state these facts add up to.
     *
     * @param rootConfigured       a Testin root is set
     * @param projectResolved      the bound project was found in the index
     * @param boundProjectMissing  the repository names a project that is nowhere under the root
     * @param cloneUrlKnown        the config says where the test project is cloned from
     * @param anyProjectsUnderRoot at least one test project folder exists under the root
     */
    public static @NotNull PanelState of(final boolean rootConfigured, final boolean projectResolved, final boolean boundProjectMissing, final boolean cloneUrlKnown, final boolean anyProjectsUnderRoot) {
        if (!rootConfigured) return NO_ROOT;
        if (projectResolved) return TREE;
        if (boundProjectMissing && cloneUrlKnown) return CLONE_BOUND;
        if (!anyProjectsUnderRoot) return NO_PROJECTS;

        return CHOOSE;
    }
}
