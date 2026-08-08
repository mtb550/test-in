package org.testin.logger;

public enum Level {
    DISABLED(-1, "OFF  "),
    TRACE(0, "TRACE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO "),
    WARN(3, "WARN "),
    ERROR(4, "ERROR"),
    FATAL(5, "FATAL");

    public final int priority;
    public final String paddedName;

    Level(final int priority, final String paddedName) {
        this.priority = priority;
        this.paddedName = paddedName;
    }
}
