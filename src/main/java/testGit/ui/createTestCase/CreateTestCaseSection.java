package testGit.ui.createTestCase;

import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.util.Set;

public interface CreateTestCaseSection {
    JPanel getWrapper();

    void showSection(final JPanel contentPanel);

    void applyTo(final TestCaseDto dto);

    void setupShortcut(final JComponent mainPanel, final JPanel slot, final CreateTestCaseBase base, final CreateTestCaseBase.UIAction repackAction, final Set<String> uniqueStepsCache);

    JComponent getFocusComponent();
}