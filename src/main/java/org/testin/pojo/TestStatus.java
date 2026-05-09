package org.testin.pojo;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestStatus {
    PASSED(
            "008000",
            " [Passed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN),
            CardHoverAction.PASSED
    ),

    FAILED(
            "FF0000", ///  TODO: change it to JBColor
            " [Failed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED),
            CardHoverAction.FAILED
    ),

    BLOCKED(
            "FFA500",
            " [Blocked]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE),
            CardHoverAction.BLOCKED
    ),

    PENDING(
            "808080",
            " [Pending]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            CardHoverAction.PENDING
    );

    /*UNTESTED(
            "808080", // todo, added by still not applied
            " [Untested]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            CardHoverAction.UNTESTED
    );*/

    private final String hex;
    private final String displayText;
    private final SimpleTextAttributes style;
    private final CardHoverAction hoverAction;
}