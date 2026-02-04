package testGit.editorPanel;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.pojo.TestCase;
import javax.swing.*;
import java.awt.*;

/**
 * Handles the rendering of each TestCase card within the JBList.
 */
public class CardCellRenderer implements ListCellRenderer<TestCase> {
    @Override
    public Component getListCellRendererComponent(final JList<? extends TestCase> list, final TestCase tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        // Create the custom card UI
        TestCaseCard card = new TestCaseCard(index, tc);
        card.getAccessibleContext().setAccessibleName("Test Case: " + tc.getTitle());

        // Apply theme-aware selection border
        if (isSelected) {
            // Use blue for selection to match IntelliJ's native feel
            card.setBorder(JBUI.Borders.customLine(JBColor.blue, 1));
        } else {
            card.setBorder(JBUI.Borders.empty(1));
        }

        return card;
    }
}