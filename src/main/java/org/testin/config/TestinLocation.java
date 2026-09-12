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

import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

/**
 * Whether a test project is shared with anyone, as {@code testin.yml} says it.
 * <p>
 * The file's own word, not something worked out from which other keys happen to
 * be filled in. That makes it possible for the file to contradict itself - to
 * say {@code remote} and give no address - so this is authoritative and the rest
 * is checked against it, rather than the other way round.
 */
public enum TestinLocation {

    /**
     * This machine only. Test projects are created here and go nowhere, which is
     * a perfectly ordinary way to work and the right default for a file that
     * says nothing.
     */
    LOCAL,

    /**
     * Shared, through whatever {@link ConnectionType} names.
     */
    REMOTE;

    /**
     * What the file said, and {@link #LOCAL} when it said nothing recognizable.
     * <p>
     * A word nobody can read is a project nobody can reach, and defaulting to
     * local means the tester keeps working on their own copy rather than having
     * the plugin guess at a server.
     */
    public static @NotNull TestinLocation of(final @NotNull String value) {
        if (value.isEmpty()) return LOCAL;

        for (final TestinLocation location : values()) {
            if (location.name().equalsIgnoreCase(value.trim())) return location;
        }

        Logger.warn("location is not 'local' or 'remote' and was read as local: " + value);
        return LOCAL;
    }

    public boolean isRemote() {
        return this == REMOTE;
    }
}
