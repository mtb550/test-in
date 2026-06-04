package org.testin.ui.testCase;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.services.TestCaseCacheService;

import javax.swing.*;
import java.awt.*;

public class ModuleSection implements ICreateTestCaseSection {
    @Getter
    private final TextFieldWithAutoCompletion<String> moduleField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public ModuleSection(final @NotNull Project project) {
        this.moduleField = new TextFieldWithAutoCompletion<>(project, new TextFieldWithAutoCompletion.StringsCompletionProvider(TestCaseCacheService.getInstance(project).getModules(), CreateTestCaseFields.MODULE.getIcon()), false, "");
        this.moduleField.setFont(fieldFont);
        this.moduleField.setPlaceholder(CreateTestCaseFields.MODULE.getPlaceholder());
        this.moduleField.setShowPlaceholderWhenFocused(true);
        this.moduleField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.MODULE.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.moduleField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        moduleField.requestFocus();
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setModule(moduleField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseModule.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return moduleField;
    }

    @Override
    public void setEditable(final boolean editable) {
        moduleField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.IUIAction repackAction) {
        moduleField.setText(dto.getModule());
    }
}