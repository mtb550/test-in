package testGit.ui.TestCase;

import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;
import testGit.util.services.TestCaseCacheService;

import javax.swing.*;
import java.awt.*;

public class ExpectedSection implements CreateTestCaseSection {
    @Getter
    private final TextFieldWithAutoCompletion<String> expectedField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public ExpectedSection() {
        this.expectedField = new TextFieldWithAutoCompletion<>(Config.getProject(), new TextFieldWithAutoCompletion.StringsCompletionProvider(TestCaseCacheService.getInstance(Config.getProject()).getExpectedResults(), CreateTestCaseFields.EXPECTED.getIcon()), false, "");

        this.expectedField.setFont(fieldFont);
        this.expectedField.setPlaceholder(CreateTestCaseFields.EXPECTED.getLabel());
        this.expectedField.setShowPlaceholderWhenFocused(true);
        this.expectedField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.EXPECTED.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.expectedField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        expectedField.requestFocus();
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setExpected(expectedField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.UIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpected.getShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return expectedField;
    }

    @Override
    public void setEditable(final boolean editable) {
        expectedField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.UIAction repackAction) {
        if (dto.getExpected() != null) {
            expectedField.setText(dto.getExpected());
        }
    }
}