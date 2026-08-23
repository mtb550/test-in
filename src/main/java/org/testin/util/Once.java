package org.testin.util;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Work that happens once per project.
 * <p>
 * Opening a project starts Testin from three doors - the platform's startup
 * extension, the tree tool window and the view tool window - and each of them
 * has to work when it is the one that got there first. So the work is written
 * once and guarded here, rather than each door knowing about the other two.
 * <p>
 * Each caller kept its own flag in the project's user data, and each read that
 * flag the same way - an absent flag is a null, so each of them tested for one,
 * and each then had to remember to set it. Asking and claiming are one call
 * here, so a caller cannot do the first without the second.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Once {

    /**
     * True the first time this key is claimed on this holder, false every time
     * afterwards.
     * <p>
     * Synchronized because the callers do not share a thread: the platform runs
     * its startup extension on a background coroutine while a tool window builds
     * its content on the EDT. Reading the flag and writing it are one step here,
     * so two arriving together cannot both be told they were first - which is
     * what a second subscription to the IDE's test events costs, every status
     * change broadcast twice.
     * <p>
     * Taken on a {@link UserDataHolder} rather than a {@code Project} because
     * that is all this needs and all a test can build: a project cannot be
     * created outside a running IDE, and the rule this enforces is worth a test
     * of its own.
     */
    public static synchronized boolean claim(final @NotNull UserDataHolder holder, final @NotNull Key<Boolean> key) {
        if (Boolean.TRUE.equals(holder.getUserData(key))) return false;

        holder.putUserData(key, Boolean.TRUE);
        return true;
    }
}
