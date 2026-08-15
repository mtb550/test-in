package org.testin.testcase.create;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class StepsSection implements CreateTestCaseSection {
    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);
    private final @NotNull Project p;
    @Getter
    private final @NotNull List<EditorTextField> stepFields;
    private final @NotNull JBPanel<?> stepsContainer;
    private final @NotNull JBPanel<?> wrapper;
    /**
     * Parent for the per-step shortcut registrations; set by the owning dialog.
     */
    @Setter
    private @Nullable Disposable parentDisposable;

    public StepsSection(final @NotNull Project p) {
        this.p = p;
        this.stepFields = new ArrayList<>();

        this.stepsContainer = new JBPanel<>();
        this.stepsContainer.setLayout(new BoxLayout(this.stepsContainer, BoxLayout.Y_AXIS));
        this.stepsContainer.setOpaque(false);

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.STEPS.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.stepsContainer, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final @NotNull JBPanel<?> contentPanel) {
        if (wrapper.getParent() == null) {
            contentPanel.add(wrapper);
        }
    }

    public void showSection(final @NotNull JBPanel<?> contentPanel, final @NotNull UIAction repackAction) {
        showSection(contentPanel);
        wrapper.setVisible(true);
        addStepField("", repackAction);
        ApplicationManager.getApplication().invokeLater(() -> {
            repackAction.execute();
            if (!stepFields.isEmpty()) {
                stepFields.getLast().requestFocus();
            }
        });
    }

    public void addStepField(final @Nullable String text, final @NotNull UIAction repackAction) {
        final TextFieldWithAutoCompletionListProvider<String> provider = new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getSteps(), CreateTestCaseFields.STEPS.getIcon());
        final EditorTextField stepField = SpellChecker.createCompletionField(p, provider, text != null ? text : "");

        stepField.setOneLineMode(true);
        stepField.setFont(fieldFont);
        stepField.setPlaceholder(CreateTestCaseFields.STEPS.getPlaceholder() + (stepFields.size() + 1));
        stepField.setShowPlaceholderWhenFocused(true);
        stepField.setBorder(JBUI.Borders.empty(6, 10));

        final JBPanel<?> stepRow = new JBPanel<>(new BorderLayout(JBUI.scale(8), 0));
        stepRow.setOpaque(false);
        stepRow.setBorder(JBUI.Borders.emptyBottom(6));

        final JBLabel removeButton = new JBLabel(AllIcons.Actions.Cancel);
        removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeButton.setToolTipText("Remove step " + Shortcuts.CreateTestCaseRemoveStep.getShortcutText());

        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                removeButton.setIcon(AllIcons.General.Remove);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                removeButton.setIcon(AllIcons.Actions.Cancel);
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                removeStepAction(stepRow, stepField, repackAction);
            }
        });

        final RemoveStepShortcutAction removeStepShortcut =
                new RemoveStepShortcutAction(stepField, () -> removeStepAction(stepRow, stepField, repackAction));

        // Tied to the dialog's disposable: rows are recreated on every fillData,
        // and unparented registrations would keep the discarded rows alive.
        if (parentDisposable != null) {
            removeStepShortcut.registerCustomShortcutSet(Shortcuts.CreateTestCaseRemoveStep.getCustomShortcut(), stepField, parentDisposable);
        } else {
            removeStepShortcut.registerCustomShortcutSet(Shortcuts.CreateTestCaseRemoveStep.getCustomShortcut(), stepField);
        }

        final JBPanel<?> buttonWrapper = new JBPanel<>(new BorderLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.setBorder(JBUI.Borders.emptyRight(4));
        buttonWrapper.add(removeButton, BorderLayout.CENTER);

        stepRow.add(stepField, BorderLayout.CENTER);
        stepRow.add(buttonWrapper, BorderLayout.EAST);

        stepFields.add(stepField);
        stepsContainer.add(stepRow);
    }

    private void removeStepAction(final @NotNull JBPanel<?> stepRow, final @NotNull EditorTextField stepField,
                                  final @NotNull UIAction repackAction) {
        if (stepFields.size() == 1) {
            stepField.setText("");
            stepField.requestFocus();
            return;
        }

        stepsContainer.remove(stepRow);
        stepFields.remove(stepField);

        for (int i = 0; i < stepFields.size(); i++)
            stepFields.get(i).setPlaceholder(CreateTestCaseFields.STEPS.getPlaceholder() + (i + 1));

        if (!stepFields.isEmpty())
            stepFields.getLast().requestFocus();
        ApplicationManager.getApplication().invokeLater(repackAction::execute);
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            final List<String> finalSteps = new ArrayList<>();
            for (final EditorTextField sf : stepFields) {
                if (!sf.getText().trim().isEmpty()) {
                    finalSteps.add(sf.getText().trim());
                }
            }
            dto.setSteps(finalSteps);
        }
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseAddStep.getCustomShortcut(), () ->
                showSection(slot, repackAction));
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        if (!stepFields.isEmpty()) {
            return stepFields.getLast();
        }
        return stepsContainer;
    }

    @Override
    public void setEditable(final boolean editable) {
        for (final EditorTextField field : stepFields) {
            field.setEnabled(editable);
            final Container row = field.getParent();
            if (row != null) {
                for (final Component c : row.getComponents()) {
                    if (c instanceof JBPanel<?> buttonWrapper) {
                        buttonWrapper.setVisible(editable);
                    }
                }
            }
        }
    }

    public void setStepsData(final @NotNull List<String> steps, final @NotNull UIAction repack) {
        stepsContainer.removeAll();
        stepFields.clear();
        for (final String step : steps) {
            addStepField(step, repack);
        }
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        setStepsData(dto.getSteps(), repackAction);
    }
}