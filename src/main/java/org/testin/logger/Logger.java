package org.testin.logger;

import com.intellij.openapi.application.ApplicationManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Logger {

    private static final @NotNull StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void setLogLevel(final @NotNull Level level) {
        getService().ifPresent(service -> service.setLogLevel(level));
    }

    public static void trace(final @NotNull String message) {
        log(Level.TRACE, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void debug(final @NotNull String message) {
        log(Level.DEBUG, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void info(final @NotNull String message) {
        log(Level.INFO, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void warn(final @NotNull String message) {
        log(Level.WARN, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void error(final @NotNull String message) {
        log(Level.ERROR, WALKER.getCallerClass().getSimpleName(), message);
    }

    /**
     * Nothing calls this yet, and it is kept deliberately.
     * <p>
     * The settings combo is built from {@code Level.values()}, so FATAL is a log
     * level a tester can already choose, stored by name and read back with
     * {@code Level.valueOf}. Deleting the level would make that stored setting
     * fail to parse on the next start; deleting only this method would leave a
     * choice in the settings that silences the log, which is what DISABLED is
     * for.
     */
    @SuppressWarnings("unused")
    public static void fatal(final @NotNull String message) {
        log(Level.FATAL, WALKER.getCallerClass().getSimpleName(), message);
    }

    private static void log(final @NotNull Level level, final @NotNull String callerClass, final @NotNull String message) {
        getService().ifPresentOrElse(
                service -> service.log(level, callerClass, message),
                () -> System.out.println("[" + level.paddedName + "] [" + callerClass + "] " + message));
    }

    /**
     * Empty before the application is up: the two callers fall back to stdout.
     * <p>
     * Asked each time rather than cached. The cache was a static mutable field -
     * the last of its kind in the plugin - and it bought nothing: a service
     * lookup is a map read, while holding one across the life of the class
     * means a disposed application is still answered for, which is exactly what
     * a test running two of them in one JVM would hit.
     */
    private static @NotNull Optional<LoggerService> getService() {
        return Optional.ofNullable(ApplicationManager.getApplication())
                .map(application -> application.getService(LoggerService.class));
    }
}
