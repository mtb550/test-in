package org.testin.logger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Logger {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // todo: why nullable and static ?
    @Nullable
    private static volatile Project p;

    private static volatile LoggerService backendService;

    @Nullable
    public static Project getProject() {
        return p;
    }

    public static void setProject(final @NotNull Project p) {
        Logger.p = p;
    }

    public static void setLogLevel(final @NotNull Level level) {
        LoggerService service = getService();
        //if (service != null) service.setLogLevel(level);
        service.setLogLevel(Level.TRACE);
    }

    public static void trace(final @NotNull String message) {
        log(Level.TRACE, WALKER.getCallerClass().getSimpleName(), message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void debug(final @NotNull String message) {
        log(Level.DEBUG, WALKER.getCallerClass().getSimpleName(), message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void info(final @NotNull String message) {
        log(Level.INFO, WALKER.getCallerClass().getSimpleName(), message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void info(final @NotNull String callerClass, final @NotNull String message) {
        log(Level.INFO, callerClass, message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void warn(final @NotNull String message) {
        log(Level.WARN, WALKER.getCallerClass().getSimpleName(), message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void error(final @NotNull String message) {
        log(Level.ERROR, WALKER.getCallerClass().getSimpleName(), message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void error(final @NotNull String callerClass, final @NotNull String message) {
        log(Level.ERROR, callerClass, message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    public static void fatal(final @NotNull String message) {
        log(Level.FATAL, WALKER.getCallerClass().getSimpleName(), message);
        System.out.println(WALKER.getCallerClass().getSimpleName() + " " + message);
    }

    private static void log(final Level level, final String callerClass, final String message) {
        LoggerService service = getService();

        if (service != null)
            service.log(level, callerClass, message);
        else
            System.out.println("[" + level.paddedName + "] [" + callerClass + "] " + message);

    }

    private static LoggerService getService() {
        if (backendService == null) {
            if (ApplicationManager.getApplication() != null) {
                backendService = ApplicationManager.getApplication().getService(LoggerService.class);
            }
        }
        return backendService;
    }

}