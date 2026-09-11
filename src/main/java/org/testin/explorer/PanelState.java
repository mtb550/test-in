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
     * The repository has a {@code testin.yml} and it could not be read.
     * <p>
     * Its own state rather than {@link #CHOOSE}, because the two need opposite
     * things of the tester: choosing is what an unbound repository offers, and
     * a broken file is a line to correct. Answered before anything else that
     * reads the file, so nothing acts on a config that said nothing because it
     * could not be parsed (#66, finding 10).
     */
    BROKEN_CONFIG,

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
     * @param configUnreadable     the repository has a testin.yml that could not be parsed
     * @param projectResolved      the bound project was found in the index
     * @param boundProjectMissing  the repository names a project that is nowhere under the root
     * @param cloneUrlKnown        the config says where the test project is cloned from
     * @param anyProjectsUnderRoot at least one test project folder exists under the root
     */
    public static @NotNull PanelState of(final boolean rootConfigured, final boolean configUnreadable, final boolean projectResolved, final boolean boundProjectMissing, final boolean cloneUrlKnown, final boolean anyProjectsUnderRoot) {
        if (!rootConfigured) return NO_ROOT;

        // Before the project is looked at, because a file that would not parse
        // is why nothing is bound. Saying "choose a test project" over a broken
        // file sends the tester to fix the wrong thing (#66, finding 10).
        if (configUnreadable) return BROKEN_CONFIG;

        if (projectResolved) return TREE;
        if (boundProjectMissing && cloneUrlKnown) return CLONE_BOUND;
        if (!anyProjectsUnderRoot) return NO_PROJECTS;

        return CHOOSE;
    }
}
