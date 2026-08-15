package org.testin.setting;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;

import java.nio.file.Path;

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
 * same root and a change made in one is visible in all of them. Keeping the
 * scope here costs nothing — a project container resolves an application-level
 * service to the same object — and saves passing the project at twenty call
 * sites that already have it (#70).
 */
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestinRoot {

    private final @NotNull Project p;

    /**
     * A stored root as a path. Missing, empty and whitespace-only values all mean
     * "no root configured" and normalize to the empty path, so callers have one
     * thing to check rather than three.
     */
    public static @NotNull Path normalize(final @Nullable String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return Path.of("");
        }
        return Path.of(rawPath.trim());
    }

    /**
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

    public void setPath(final @Nullable Path path) {
        final AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        settings.rootTestinPath = path != null ? path.toString() : "";
    }
}
