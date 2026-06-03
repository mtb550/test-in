package org.testin.util.logger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Log {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Nullable
    private static volatile Project project;

    private static volatile LoggerService backendService;

    @Nullable
    public static Project getProject() {
        return project;
    }

    public static void setProject(@NotNull final Project project) {
        Log.project = project;
    }

    public static void setLogLevel(@NotNull final Level level) {
        LoggerService service = getService();
        if (service != null) service.setLogLevel(level);
    }

    public static void debug(@NotNull final String message) {
        log(Level.DEBUG, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void info(@NotNull final String message) {
        log(Level.INFO, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void warn(@NotNull final String message) {
        log(Level.WARN, WALKER.getCallerClass().getSimpleName(), message);
    }

    public static void error(@NotNull final String message) {
        log(Level.ERROR, WALKER.getCallerClass().getSimpleName(), message);
    }

    private static void log(final Level level, final String callerClass, final String message) {
        LoggerService service = getService();

        if (service != null) {
            service.log(level, callerClass, message);
        } else {
            System.out.println("[" + level.paddedName + "] [" + callerClass + "] " + message);
        }
    }

    private static LoggerService getService() {
        if (backendService == null) {
            if (ApplicationManager.getApplication() != null) {
                backendService = ApplicationManager.getApplication().getService(LoggerService.class);
            }
        }
        return backendService;
    }

    public enum Level {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO "),
        WARN(2, "WARN "),
        ERROR(3, "ERROR");

        public final int priority;
        public final String paddedName;

        Level(final int priority, final String paddedName) {
            this.priority = priority;
            this.paddedName = paddedName;
        }
    }
}
