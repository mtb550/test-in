package org.testin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Work that happens once per project.
 * <p>
 * Three things must happen the first time and never again: subscribing to the
 * IDE's test events, warning that no Java test source root exists, and warning
 * that an optional plugin is missing. Each kept its own flag in the project's
 * user data, and each read that flag the same way - an absent flag is a null,
 * so each of them tested for one, and each then had to remember to set it.
 * <p>
 * Asking and claiming are one call here, so a caller cannot do the first
 * without the second.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Once {

    /**
     * True the first time this key is claimed for this project, false every time
     * afterwards.
     */
    public static boolean claim(final @NotNull Project p, final @NotNull Key<Boolean> key) {
        if (Boolean.TRUE.equals(p.getUserData(key))) return false;

        p.putUserData(key, Boolean.TRUE);
        return true;
    }
}
