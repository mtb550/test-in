package testGit.pojo;

import lombok.Getter;

import java.util.Set;
import java.util.function.BiConsumer;

@Getter
public enum Group {
    UNASSIGNED(
            "<No Group>",
            false,
            false
    ),

    REGRESSION(
            "Regression",
            true,
            true
    ),

    SMOKE(
            "Smoke",
            true,
            true
    ),

    SANITY(
            "Sanity",
            true,
            true
    ),

    SECURITY(
            "Security",
            false,
            true
    ),

    UI(
            "UI",
            false,
            true
    ),

    FUNCTIONAL(
            "Functional",
            false,
            true
    ),

    VALIDATION(
            "Validation",
            false,
            true
    );

    private final String name;
    private final boolean active;
    private final boolean assignable;
    private final BiConsumer<Set<Group>, Boolean> action;

    Group(final String name, final boolean active, final boolean assignable) {
        this.name = name;
        this.active = active;
        this.assignable = assignable;

        this.action = (set, state) -> {
            if (state) set.add(this);
            else set.remove(this);
        };
    }

    public void onChange(final Set<Group> set, final boolean state) {
        action.accept(set, state);
    }
}