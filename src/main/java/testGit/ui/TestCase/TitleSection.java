package testGit.ui.TestCase;

import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;
import testGit.util.cache.TestCaseCacheService;

import javax.swing.*;
import java.awt.*;

public class TitleSection implements CreateTestCaseSection {
    @Getter
    private final TextFieldWithAutoCompletion<String> titleField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);

    public TitleSection() {
        this.titleField = new TextFieldWithAutoCompletion<>(Config.getProject(), new TextFieldWithAutoCompletion.StringsCompletionProvider(TestCaseCacheService.getInstance(Config.getProject()).getTitles(), CreateField.TITLE.getIcon()), false, "");

        this.titleField.setFont(fieldFont);
        this.titleField.setPlaceholder(CreateField.TITLE.getLabel());
        this.titleField.setShowPlaceholderWhenFocused(true);
        this.titleField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateField.TITLE.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.titleField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    public void setError(final boolean error) {
        if (error) {
            titleField.setForeground(JBColor.RED);
            titleField.requestFocus();
        } else
            titleField.setBackground(UIUtil.getTextFieldBackground());
        titleField.repaint();
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        titleField.requestFocus();
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null && titleField.isEnabled())
            dto.setTitle(titleField.getText().trim());
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.UIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseTitle.getShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return titleField;
    }

    @Override
    public void setEditable(final boolean editable) {
        titleField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.UIAction repackAction) {
        if (dto.getTitle() != null) {
            titleField.setText(dto.getTitle());
        }
    }
}