package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ViewTab {
    DETAILS("Details"),
    HISTORY("History"),
    OPEN_BUGS("Open Bugs");

    private final String displayName;

}