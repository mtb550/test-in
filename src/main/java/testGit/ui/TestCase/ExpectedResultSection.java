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

public class ExpectedResultSection implements ICreateTestCaseSection {
    @Getter
    private final TextFieldWithAutoCompletion<String> expectedResultField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public ExpectedResultSection() {
        this.expectedResultField = new TextFieldWithAutoCompletion<>(Config.getProject(), new TextFieldWithAutoCompletion.StringsCompletionProvider(TestCaseCacheService.getInstance(Config.getProject()).getExpectedResults(), CreateTestCaseFields.EXPECTED_RESULT.getIcon()), false, "");
        this.expectedResultField.setFont(fieldFont);
        this.expectedResultField.setPlaceholder(CreateTestCaseFields.EXPECTED_RESULT.getName());
        this.expectedResultField.setShowPlaceholderWhenFocused(true);
        this.expectedResultField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.EXPECTED_RESULT.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.expectedResultField, BorderLayout.CENTER);
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
        expectedResultField.requestFocus();
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setExpectedResult(expectedResultField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpected.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return expectedResultField;
    }

    @Override
    public void setEditable(final boolean editable) {
        expectedResultField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.IUIAction repackAction) {
        expectedResultField.setText(dto.getExpectedResult());
    }
}