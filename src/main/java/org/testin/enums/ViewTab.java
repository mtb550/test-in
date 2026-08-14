package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum ViewTab {
    DETAILS("Details"),

    // Reported as never used, and kept: these two tabs are declared and not yet
    // built, not dead. The view panel renders DETAILS only (ViewPanel:110) until
    // the history and bug views land (#61).
    HISTORY("History"),
    OPEN_BUGS("Open Bugs");

    private final @NotNull String displayName;

}
