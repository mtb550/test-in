package org.testin.testCase.createDialog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateTestCaseFields;
import org.testin.enums.IUIAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;

public class ModuleSection implements ICreateTestCaseSection {
    @Getter
    private final TextFieldWithAutoCompletion<String> moduleField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public ModuleSection(final @NotNull Project p) {
        this.moduleField = new TextFieldWithAutoCompletion<>(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getModules(), CreateTestCaseFields.MODULE.getIcon()), false, "");
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
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseBaseDialog base, final IUIAction repackAction) {
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
    public void fillData(final TestCaseDto dto, final IUIAction repackAction) {
        moduleField.setText(dto.getModule());
    }
}