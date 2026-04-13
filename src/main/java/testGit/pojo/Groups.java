package testGit.pojo;

import lombok.Getter;

import java.util.Set;
import java.util.function.BiConsumer;

@Getter
public enum Groups {
    REGRESSION(
            "Regression",
            true
    ),

    SMOKE(
            "Smoke",
            true
    ),

    SANITY(
            "Sanity",
            true),

    SECURITY(
            "Security",
            false),

    UI(
            "UI",
            false
    ),

    FUNCTIONAL(
            "Functional",
            false
    ),

    VALIDATION(
            "Validation",
            false
    );

    private final String name;
    private final boolean active;
    private final BiConsumer<Set<Groups>, Boolean> action;

    Groups(final String name, final boolean active) {
        this.name = name;
        this.active = active;

        this.action = (set, state) -> {
            if (state) set.add(this);
            else set.remove(this);
        };
    }

    public void onChange(final Set<Groups> set, final boolean state) {
        action.accept(set, state);
    }
}