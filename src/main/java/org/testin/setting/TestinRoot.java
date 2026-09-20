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

package org.testin.setting;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestinRoot {
    private final @NotNull Project p;

    public static final @NotNull Path NONE = Path.of("");

    // UC-SETTING-002, Rule-SETTING-011
    public static @NotNull Path normalize(final @Nullable String rawPath) {
        return Path.of(Objects.requireNonNullElse(rawPath, "").trim());
    }

    // UC-SETTING-002, Rule-SETTING-011
    public static boolean isConfigured(final @NotNull Path root) {
        return !NONE.equals(root);
    }

    // Rule-SETTING-004
    public static boolean isRootChanged(final @Nullable String before, final @Nullable String after) {
        return !normalize(before).equals(normalize(after));
    }

    public @NotNull Path getPath() {
        return normalize(Services.getInstance(p, AppSettingsState.class).rootTestinPath);
    }

    // UC-SETTING-002, Rule-SETTING-011
    public boolean isConfigured() {
        return isConfigured(getPath());
    }

    // UC-SETTING-002, Rule-SETTING-013
    public @NotNull Path absolutePath() {
        final @NotNull Path root = getPath();
        if (!isConfigured(root)) return NONE;

        return root.isAbsolute() ? root : basePath().resolve(root);
    }

    private @NotNull Path basePath() {
        return Path.of(Objects.toString(p.getBasePath(), ""));
    }

    // UC-SETTING-002, Rule-SETTING-010
    public @NotNull Path resolve(final @NotNull List<String> segments) {
        Path resolved = isConfigured() ? absolutePath() : basePath();

        for (final String segment : segments) {
            resolved = resolved.resolve(segment);
        }

        return resolved;
    }
}
