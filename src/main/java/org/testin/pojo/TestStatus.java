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
            CardHoverAction.PASSED,
            JBColor.GREEN.darker()

    ),

    FAILED(
            "FF0000", ///  TODO: change it to JBColor
            " [Failed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED),
            CardHoverAction.FAILED,
            JBColor.RED.darker()
    ),

    BLOCKED(
            "FFA500",
            " [Blocked]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE),
            CardHoverAction.BLOCKED,
            JBColor.ORANGE.darker()
    ),

    PENDING(
            "808080",
            " [Pending]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            CardHoverAction.PENDING,
            null
    ),

    UNTESTED(
            "808080",
            " [Untested]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            CardHoverAction.UNTESTED,
            JBColor.GRAY.darker()
    );

    private final String hex;
    private final String displayText;
    private final SimpleTextAttributes style;
    private final CardHoverAction hoverAction;
    private final Color rowColor;
}