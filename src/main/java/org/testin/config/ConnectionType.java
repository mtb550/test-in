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
    NONE(
            false
    ),

    /**
     * A Git repository. There is no server to sync to, so that action is off.
     * Whether the branch box shows is the folder's answer, not this one's: a
     * Git folder has branches whatever the file says (Rule-TREE-PANEL-108).
     */
    GIT(
            false
    ),

    /**
     * An SFTP server. There are no branches to choose and nothing to fetch, so
     * the branch box is not shown and nothing here ever reaches a Git remote.
     */
    SFTP(
            true
    );

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
