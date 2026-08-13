package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum ViewTab {
    DETAILS("Details"),
    HISTORY("History"),
    OPEN_BUGS("Open Bugs");

    private final @NotNull String displayName;

}
