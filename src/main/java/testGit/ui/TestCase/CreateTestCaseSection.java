package testGit.ui.TestCase;

import com.intellij.util.ui.JBUI;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public interface CreateTestCaseSection {
    JPanel getWrapper();

    void showSection(final JPanel contentPanel);

    void applyTo(final TestCaseDto dto);

    void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.UIAction repackAction);

    JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final TestCaseDto dto, final TestCaseUIBase.UIAction repackAction);

    default JPanel createIconPanel(final Icon icon) {
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}