package org.testin.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

/**
 * How a shared test project is reached, as {@code testin.yml} says it (#94).
 * <p>
 * Each constant carries what follows from it, so nothing asks "which connection
 * is this" and then branches - a caller asks the connection.
 */
@Getter
@AllArgsConstructor
public enum ConnectionType {

    /**
     * Not shared at all. What a project on this machine only reports, so every
     * reader has a value rather than a question about whether one was set.
     */
    NONE(false, false, false),

    /**
     * A Git repository. Branches mean something here, so the branch box is
     * shown and a refresh brings the remote up to date - and there is no server
     * to sync to, so that action is off.
     */
    GIT(true, true, false),

    /**
     * An SFTP server. There are no branches to choose and nothing to fetch, so
     * the branch box is not shown and nothing here ever reaches a Git remote.
     */
    SFTP(false, false, true);

    /**
     * Whether a branch box belongs on screen. A project with no branches showing
     * a box that says it has none is a row of screen explaining something that
     * was never true of it.
     */
    private final boolean showsBranches;

    /**
     * Whether redrawing the panel should bring a Git remote up to date.
     */
    private final boolean fetchesOnRefresh;

    /**
     * Whether there is a server to send this project to. What the sync action
     * asks before it offers itself, so a repository that shares through Git is
     * never given a button that could only ever answer that it has no server.
     */
    private final boolean syncsToServer;

    /**
     * What the file said, and {@link #NONE} when it said nothing recognizable.
     */
    public static @NotNull ConnectionType of(final @NotNull String value) {
        if (value.isEmpty()) return NONE;

        for (final ConnectionType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) return type;
        }

        Logger.warn("connection is not 'git' or 'sftp' and was read as none: " + value);
        return NONE;
    }
}
