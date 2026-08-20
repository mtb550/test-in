package org.testin.testcase.create;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.statusbar.StatusBarItem;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public abstract class TestCaseBaseDialog {
    protected final @NotNull Project p;
    protected final @NotNull DescriptionSection DescriptionSection;
    protected final @NotNull ExpectedResultSection expectedResultSection;
    protected final @NotNull ModuleSection moduleSection;
    protected final @NotNull TestDataSection testDataSection;
    protected final @NotNull PreConditionsSection preConditionsSection;
    protected final @NotNull PrioritySection prioritySection;
    protected final @NotNull GroupSection groupSection;
    protected final @NotNull StepsSection stepsSection;
    protected final @NotNull StatusBarSection statusBarSection;
    /**
     * Owns all global registrations of this dialog (application focus listener,
     * per-step shortcuts). Parented to the project, so everything is released
     * even when the popup is torn down without firing onClosed.
     */
    protected final @NotNull Disposable dialogDisposable;
    protected final @NotNull Map<CreateTestCaseSection, StatusBarItem[]> statusBarMapping;
    private final @NotNull List<CreateTestCaseSection> cachedSections;
    /**
     * A focus change nothing listens for, before the dynamic status bar is
     * installed.
     */
    private static final @NotNull PropertyChangeListener NOTHING_ON_FOCUS = evt -> {
    };

    /**
     * What a focus change updates the status bar with, and one that updates
     * nothing before the dynamic bar is installed. Removing a listener that was
     * never added is what the focus manager does with it: nothing.
     */
    private @NotNull PropertyChangeListener focusListener = NOTHING_ON_FOCUS;

    public TestCaseBaseDialog(final @NotNull Project p) {
        this.p = p;
        this.dialogDisposable = Disposer.newDisposable("testin.testCaseDialog");
        Disposer.register(p, dialogDisposable);

        this.DescriptionSection = new DescriptionSection(p);
        this.expectedResultSection = new ExpectedResultSection(p);
        this.moduleSection = new ModuleSection(p);
        this.testDataSection = new TestDataSection();
        this.preConditionsSection = new PreConditionsSection(p);
        this.stepsSection = new StepsSection(p, dialogDisposable);
        this.prioritySection = new PrioritySection();
        this.groupSection = new GroupSection();
        this.statusBarSection = new StatusBarSection();

        this.cachedSections = Arrays.stream(CreateTestCaseFields.values())
                .map(field -> field.getSectionExtractor().apply(this))
                .toList();

        this.statusBarMapping = Arrays.stream(CreateTestCaseFields.values())
                .collect(Collectors.toMap(
                        field -> field.getSectionExtractor().apply(this),
                        CreateTestCaseFields::getStatusBarItems
                ));

    }

    protected void initDynamicStatusBar(final @NotNull JComponent parentPanel) {
        focusListener = evt -> {
            final Component focusOwner = (Component) evt.getNewValue();
            if (focusOwner != null && UIUtil.isDescendingFrom(focusOwner, parentPanel)) {
                for (final CreateTestCaseSection section : getAllSections()) {
                    if (UIUtil.isDescendingFrom(focusOwner, section.getWrapper())) {
                        final StatusBarItem[] items = statusBarMapping.getOrDefault(section, statusBarMapping.get(DescriptionSection));
                        if (items != null) statusBarSection.updateItems(items);
                        return;
                    }
                }
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener);

        // Removal runs on any disposal path (popup onClosed or project teardown).
        Disposer.register(dialogDisposable, this::removeFocusListener);
    }

    private void removeFocusListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
        focusListener = NOTHING_ON_FOCUS;
    }

    public void dispose() {
        Disposer.dispose(dialogDisposable);
    }

    public @NotNull List<CreateTestCaseSection> getAllSections() {
        return cachedSections;
    }

    public void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull UIAction action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                action.execute();
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                if (LookupManager.getInstance(p).getActiveLookup() != null) {
                    e.getPresentation().setEnabled(false);
                    return;
                }
                if (prioritySection.getCombo().isPopupVisible()) {
                    e.getPresentation().setEnabled(false);
                    return;
                }

                e.getPresentation().setEnabled(true);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(shortcutSet, component);
    }

    public @NotNull Runnable save(final @NotNull TestCaseDto dto, final @NotNull Consumer<@NotNull TestCaseDto> onSave, final @NotNull JBPopup[] popupWrapper) {
        return () -> {
            getAllSections().forEach(section -> section.applyTo(dto));

            final String title = dto.getDescription();
            if (DescriptionSection.getWrapper().getParent() == null || !title.trim().isEmpty()) {
                onSave.accept(dto);

                popupWrapper[0].closeOk(null);

            } else
                DescriptionSection.setError(true);
        };
    }

}