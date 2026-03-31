package testGit.pojo;

import lombok.Getter;

@Getter
public enum ViewTab {
    DETAILS("Details"),
    HISTORY("History"),
    OPEN_BUGS("Open Bugs");

    private final String displayName;

    ViewTab(final String displayName) {
        this.displayName = displayName;
    }
}