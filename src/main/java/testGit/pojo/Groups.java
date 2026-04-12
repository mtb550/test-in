package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Groups {
    REGRESSION("Regression", true),
    SMOKE("Smoke", true),
    SANITY("Sanity", true),
    SECURITY("Security", false),
    UI("UI", false),
    FUNCTIONAL("Functional", false),
    VALIDATION("Validation", false);

    private final String displayName;
    private final boolean active;
}