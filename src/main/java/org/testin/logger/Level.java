package org.testin.logger;

import lombok.AllArgsConstructor;

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
    public final String paddedName;

}
