package org.testin.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;

import java.nio.file.Path;

@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class Setting {

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

    public @NotNull Path getTestinPath() {
        return normalize(Services.getInstance(p, AppSettingsState.class).rootTestinPath);
    }

    public void setTestinPath(final @Nullable Path path) {
        final AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        settings.rootTestinPath = path != null ? path.toString() : "";
    }
}
