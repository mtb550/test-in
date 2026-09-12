package org.testin.logger;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AllArgsConstructor
public enum Level {
    DISABLED(-1, "OFF  "),
    TRACE(0, "TRACE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO "),
    WARN(3, "WARN "),
    ERROR(4, "ERROR"),
    FATAL(5, "FATAL");

    public final int priority;
    public final @NotNull String paddedName;

    /**
     * UC-SETTING-007, Rule-SETTING-025.
     * <p>
     * The stored word when it still names a level, and INFO when it does not -
     * a level written by an older build, one renamed since, or one typed into
     * {@code testinSettings.xml} by hand.
     * <p>
     * Here rather than at the reader, because the reader is
     * {@code Level.valueOf} in the startup activity: an unknown word threw out
     * of it after the once-per-project claim had been spent, so nothing indexed,
     * no door retried, and the tester had an empty tree for the session with one
     * line in {@code idea.log} to say why (#66, finding 87).
     */
    public static @NotNull String known(final @NotNull String stored) {
        return Arrays.stream(values()).map(Level::name).filter(stored::equals).findFirst().orElse(INFO.name());
    }
}
