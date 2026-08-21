package org.testin.setting;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;

import java.nio.file.Path;
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
     * The path of a project with no root configured. Empty, blank and never-set
     * settings all normalize to it, so there is one thing to compare against
     * rather than three ways of being absent.
     */
    public static final @NotNull Path NONE = Path.of("");

    /**
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
     * Whether a root has been configured at all. Asked by name so no caller has
     * to know that "not configured" is spelled as the empty path.
     */
    public static boolean isConfigured(final @NotNull Path root) {
        return !NONE.equals(root);
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

    public void setPath(final @NotNull Path path) {
        final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        settings.rootTestinPath = path.toString();
    }
}
