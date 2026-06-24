package org.testin.pojo;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum TestStatus {
    PASSED(
            "008000",
            " [Passed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN),
            JBColor.GREEN

    ),

    FAILED(
            "FF0000",
            " [Failed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED),
            JBColor.RED.darker()
    ),

    BLOCKED(
            "FFA500",
            " [Blocked]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE),
            JBColor.ORANGE
    ),

    PENDING(
            "808080",
            " [Pending]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            null
    ),

    UNTESTED(
            "808080",
            " [Untested]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            JBColor.GRAY.brighter()
    );

    private final String hex;
    private final String displayText;
    private final SimpleTextAttributes style;
    private final Color rowColor;
}