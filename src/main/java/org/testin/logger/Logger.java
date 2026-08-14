package org.testin.logger;

import com.intellij.openapi.application.ApplicationManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Logger {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static volatile LoggerService backendService;

    public static void setLogLevel(final @NotNull Level level) {
        LoggerService service = getService();
        if (service != null) service.setLogLevel(level);
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

    private static void log(final Level level, final String callerClass, final String message) {
        LoggerService service = getService();

        if (service != null)
            service.log(level, callerClass, message);
        else
            System.out.println("[" + level.paddedName + "] [" + callerClass + "] " + message);
    }

    /**
     * Null before the application is up: callers fall back to stdout.
     */
    private static @Nullable LoggerService getService() {
        if (backendService == null) {
            if (ApplicationManager.getApplication() != null) {
                backendService = ApplicationManager.getApplication().getService(LoggerService.class);
            }
        }
        return backendService;
    }
}
