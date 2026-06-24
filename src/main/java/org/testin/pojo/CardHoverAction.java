package org.testin.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.util.KeyboardSet;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE("Navigate to Code",
            KeyboardSet.NavigateToCode,
            "Navigate to Code Shift+F5"
    ),

    RUN("Run Test Case",
            KeyboardSet.RunTestCase,
            "Run Test Case F5"
    );

    private final String tooltip;
    private final KeyboardSet keyboardSet;
    private final String hintText;
}