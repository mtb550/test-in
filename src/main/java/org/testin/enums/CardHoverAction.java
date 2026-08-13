package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE_TO_TEST_METHOD("Navigate to Code",
            Shortcuts.NavigateToCode,
            "Navigate to Code Shift+F5"
    ),

    RUN_TEST_CASE("Run Test Case",
            Shortcuts.RunTestCase,
            "Run Test Case F5"
    );

    private final @NotNull String tooltip;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull String hintText;
}
