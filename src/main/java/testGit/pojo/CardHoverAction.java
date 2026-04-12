package testGit.pojo;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE("Navigate to Code",
            null,
            null
    ),

    RUN("Run Test Case",
            null,
            null
    ),

    PASSED("Mark as Passed", /// TODO: change to use JBColor.[Red.brighter()], better that current.
            new JBColor(new Color(39, 174, 96, 40), new Color(46, 125, 50, 60)),
            new JBColor(new Color(39, 174, 96), new Color(129, 199, 132))
    ),

    FAILED("Mark as Failed",
            new JBColor(new Color(192, 57, 43, 40), new Color(183, 28, 28, 60)),
            new JBColor(new Color(192, 57, 43), new Color(229, 115, 115))
    ),

    BLOCKED("Mark as Blocked",
            new JBColor(new Color(243, 156, 18, 40), new Color(237, 108, 2, 60)),
            new JBColor(new Color(243, 156, 18), new Color(255, 183, 77))
    ),

    PENDING("Pending Status",
            new JBColor(new Color(128, 128, 128, 40), new Color(128, 128, 128, 60)),
            JBColor.GRAY
    );

    private final String tooltip;
    private final JBColor background;
    private final JBColor foreground;
}