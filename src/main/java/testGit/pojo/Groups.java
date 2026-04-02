package testGit.pojo;

import lombok.Getter;

@Getter
public enum Groups {
    Regression(true),
    Smoke(true),
    Sanity(true),
    Security(false),
    UI(false),
    Functional(false),
    Validation(false);

    private final boolean active;

    Groups(boolean active) {
        this.active = active;
    }

}