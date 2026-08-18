package org.testin.editor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.OptionalPlugin;
import org.testin.util.Shortcuts;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE_TO_TEST_METHOD("Navigate to Code",
            Shortcuts.NavigateToCode,
            "Navigate to Code Shift+F5",
            OptionalPlugin.JAVA
    ),

    RUN_TEST_CASE("Run Test Case",
            Shortcuts.RunTestCase,
            "Run Test Case F5",
            OptionalPlugin.TESTNG
    );

    private final @NotNull String tooltip;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull String hintText;
    /**
     * The IDE plugin this action needs to do anything at all.
     */
    private final @NotNull OptionalPlugin requires;

    /**
     * Whether this IDE offers the action. Asked before the icon is drawn and
     * before the pointer is asked what it is over, so in PyCharm or GoLand the
     * icon is absent rather than present and answering with a balloon (#66).
     */
    public boolean isOffered() {
        return requires.isAvailable();
    }
}
