package testGit.ui.single.nnew;

import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;

public interface CreateTestCaseSection {
    JPanel getWrapper();

    void showSection(final JPanel contentPanel);

    void applyTo(final TestCaseDto dto);
}
