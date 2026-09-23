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

package org.testin.indexer;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Service(Service.Level.APP)
public final class OwnWrites {
    private static final byte[] NOTHING_TO_COMPARE = new byte[0];

    private static final long SETTLES_IN_MILLIS = 5_000;
    private final @NotNull Map<String, Claim> written = new ConcurrentHashMap<>();

    private static boolean stillSays(final @NotNull Path path, final byte @NotNull [] ourContent) {
        try {
            return Arrays.equals(Files.readAllBytes(path), ourContent);
        } catch (final IOException stillSettling) {
            Logger.debug("Could not read " + path.getFileName() + " to tell our write from an edit: " + stillSettling.getMessage());
            return true;
        }
    }

    private static @NotNull String key(final @NotNull Path path) {
        final @NotNull String full = path.toAbsolutePath().normalize().toString();

        return SystemInfo.isFileSystemCaseSensitive ? full : full.toLowerCase(Locale.ROOT);
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019
    public void record(final @NotNull Project p, final @NotNull Path path) {
        record(p.getLocationHash(), path);
    }

    void record(final @NotNull String window, final @NotNull Path path) {
        forgetOldEntries();
        written.put(key(path), new Claim(System.currentTimeMillis(), NOTHING_TO_COMPARE, window));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019, Rule-INTERNAL-064
    public void wrote(final @NotNull Project p, final @NotNull Path path, final byte @NotNull [] content) {
        wrote(p.getLocationHash(), path, content);
    }

    void wrote(final @NotNull String window, final @NotNull Path path, final byte @NotNull [] content) {
        forgetOldEntries();
        written.put(key(path), new Claim(System.currentTimeMillis(), content, window));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019, Rule-INTERNAL-064
    // UC-INTERNAL-003, Rule-INTERNAL-019
    public boolean areOurs(final @NotNull Path path, final @NotNull Project p) {
        return areOurs(path, p.getLocationHash());
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019
    boolean areOurs(final @NotNull Path path, final @NotNull String window) {
        final @NotNull Optional<Claim> claim = Optional.ofNullable(written.get(key(path)))
                .filter(one -> System.currentTimeMillis() - one.at() < SETTLES_IN_MILLIS)
                .filter(one -> one.by().equals(window));

        if (claim.isEmpty()) return false;

        final byte @NotNull [] ourContent = claim.orElseThrow().content();

        return ourContent.length == 0 || stillSays(path, ourContent);
    }

    private void forgetOldEntries() {
        final long now = System.currentTimeMillis();
        written.values().removeIf(one -> now - one.at() >= SETTLES_IN_MILLIS);
    }

    private record Claim(long at, byte @NotNull [] content, @NotNull String by) {
    }
}
