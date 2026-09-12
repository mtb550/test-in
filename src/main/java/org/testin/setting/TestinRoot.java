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

/**
 * The Testin root, as a path.
 * <p>
 * Separate from {@link AppSettingsState} on purpose, and not merely because it
 * always has been: that class is the persisted shape — the String and boolean
 * fields that are in testinSettings.xml — while this one is the logic over one
 * of them. The asymmetry is the point. A root is stored as a String that may be
 * missing, empty, blank or untrimmed, and is used as a normalized Path; turning
 * one into the other is this class, not the file format.
 * <p>
 * Project-level for the convenience of callers that already hold a project, not
 * because the value is per-project. It reads {@link AppSettingsState}, which is
 * an application-level service over one file, so every open project sees the
 * same root and a change made in one is visible in all of them. The scope here
 * costs nothing and saves passing the project at twenty call sites that already
 * have it (#70).
 * <p>
 * <b>The application-level service is reached through {@link Services}, and that
 * is not a convenience.</b> A project container asked for an application service
 * does not hand back the application's - it builds a second instance inside the
 * project, with its own persisted state, which is the bug that produced two
 * files called testinSettings.xml holding different tester names.
 * {@code Services.getInstance} is what decides which container to ask, and its
 * javadoc records what happened when nothing did.
 */
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestinRoot {

    private final @NotNull Project p;

    /**
     * The path of a project with no root configured. Empty, blank and never-set
     * settings all normalize to it, so there is one thing to compare against
     * rather than three ways of being absent.
     */
    public static final @NotNull Path NONE = Path.of("");

    /**
     * UC-SETTING-002, Rule-SETTING-011.
     * <p>
     * A stored root as a path, and {@link #NONE} when nothing is stored.
     * <p>
     * Nullable on purpose, and the only place here that is: this reads a value
     * out of the settings XML, where the entry can be missing altogether. A
     * missing entry, an empty one and a whitespace one are one answer.
     */
    public static @NotNull Path normalize(final @Nullable String rawPath) {
        return Path.of(Objects.requireNonNullElse(rawPath, "").trim());
    }

    /**
     * UC-SETTING-002, Rule-SETTING-011.
     * <p>
     * Whether a root has been configured at all. Asked by name so no caller has
     * to know that "not configured" is spelled as the empty path.
     */
    public static boolean isConfigured(final @NotNull Path root) {
        return !NONE.equals(root);
    }

    /**
     * Rule-SETTING-004.
     * <p>
     * True when an Apply moved the Testin root. The tree is built from the indexer,
     * so only a different root makes a reload necessary - every other setting is
     * read live where it is used.
     */
    public static boolean isRootChanged(final @Nullable String before, final @Nullable String after) {
        return !normalize(before).equals(normalize(after));
    }

    public @NotNull Path getPath() {
        return normalize(Services.getInstance(p, AppSettingsState.class).rootTestinPath);
    }

    /**
     * UC-SETTING-002, Rule-SETTING-011.
     * <p>
     * Whether this project has a root configured.
     * <p>
     * Six call sites asked it as {@code getPath().toString().isEmpty()} and one
     * asked the stored string directly - and those two are not the same
     * question. A root of nothing but spaces is empty once normalized and not
     * empty as stored, so the tester got the "configure the root" warning
     * alongside a log line announcing that defaults were being saved.
     */
    public boolean isConfigured() {
        return isConfigured(getPath());
    }

    /**
     * UC-SETTING-002, Rule-SETTING-013.
     * <p>
     * The configured root as an absolute path, and {@link #NONE} when none is
     * configured.
     * <p>
     * A relative root is resolved against the open project, which is how it has
     * always been read. Here rather than at each caller: the indexer had its own
     * copy of these four lines, and {@code resolve} below had a third.
     */
    public @NotNull Path absolutePath() {
        final @NotNull Path root = getPath();
        if (!isConfigured(root)) return NONE;

        return root.isAbsolute() ? root : basePath().resolve(root);
    }

    /**
     * The open project's own directory. The platform answers null for a project
     * that has none, which is the empty path here.
     */
    private @NotNull Path basePath() {
        return Path.of(Objects.toString(p.getBasePath(), ""));
    }

    /**
     * UC-SETTING-002, Rule-SETTING-010.
     * <p>
     * Where a node named by its place in the tree lives on disk.
     * <p>
     * The tree carries a node's path as the segments a tester reads - "test
     * project", "Test Runs", "cycle 1" - and every surface handed one of those
     * needs the same three steps to reach the file: fall back to the project
     * directory when no root is configured, resolve a relative root against it,
     * then walk the segments. Two places did it, and a third was about to.
     */
    public @NotNull Path resolve(final @NotNull List<String> segments) {
        Path resolved = isConfigured() ? absolutePath() : basePath();

        for (final String segment : segments) {
            resolved = resolved.resolve(segment);
        }

        return resolved;
    }

    public void setPath(final @NotNull Path path) {
        final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        settings.rootTestinPath = path.toString();
    }
}
