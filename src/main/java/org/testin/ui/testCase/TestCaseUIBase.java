package org.testin.ui.testCase;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.autoGenerator.CodeGenerator;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.statusBar.IStatusBarItem;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Getter
public abstract class TestCaseUIBase {
    protected final CodeGenerator codeGenerator;
    protected final DescriptionSection DescriptionSection;
    protected final ExpectedResultSection expectedResultSection;
    protected final ModuleSection moduleSection;
    protected final TestDataSection testDataSection;
    protected final PreConditionsSection preConditionsSection;
    protected final PrioritySection prioritySection;
    protected final GroupSection groupSection;
    protected final StepsSection stepsSection;
    protected final StatusBarSection statusBarSection;
    private final List<ICreateTestCaseSection> cachedSections;
    protected Map<ICreateTestCaseSection, IStatusBarItem[]> statusBarMapping;
    private PropertyChangeListener focusListener;

    public TestCaseUIBase(final @NotNull Project project, final @NotNull GeneratorType generatorType) {
        this.codeGenerator = new CodeGenerator(generatorType);
        this.DescriptionSection = new DescriptionSection(project);
        this.expectedResultSection = new ExpectedResultSection(project);
        this.moduleSection = new ModuleSection(project);
        this.testDataSection = new TestDataSection();
        this.preConditionsSection = new PreConditionsSection();
        this.stepsSection = new StepsSection(project);
        this.prioritySection = new PrioritySection();
        this.groupSection = new GroupSection();
        this.statusBarSection = new StatusBarSection();

        this.cachedSections = Arrays.stream(CreateTestCaseFields.values())
                .filter(CreateTestCaseFields::isCreateMenuItem)
                .map(field -> field.getSectionExtractor().apply(this))
                .toList();

        this.statusBarMapping = Arrays.stream(CreateTestCaseFields.values())
                .filter(CreateTestCaseFields::isCreateMenuItem)
                .collect(Collectors.toMap(
                        field -> field.getSectionExtractor().apply(this),
                        CreateTestCaseFields::getStatusBarItems
                ));
    }

    protected void initDynamicStatusBar(JComponent parentPanel) {
        focusListener = evt -> {
            Component focusOwner = (Component) evt.getNewValue();
            if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, parentPanel)) {
                for (ICreateTestCaseSection section : getAllSections()) {
                    if (SwingUtilities.isDescendingFrom(focusOwner, section.getWrapper())) {
                        IStatusBarItem[] items = statusBarMapping.getOrDefault(section, statusBarMapping.get(DescriptionSection));
                        if (items != null) statusBarSection.updateItems(items);
                        return;
                    }
                }
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener);
    }

    public void dispose() {
        if (focusListener != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
            focusListener = null;
        }
    }

    public List<ICreateTestCaseSection> getAllSections() {
        return cachedSections;
    }

    public void registerShortcut(final JComponent component, final CustomShortcutSet shortcutSet, final IUIAction action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                action.execute();
            }

            @Override
            public void update(@NotNull final AnActionEvent e) {
                if (e.getProject() != null && LookupManager.getInstance(e.getProject()).getActiveLookup() != null) {
                    e.getPresentation().setEnabled(false);
                    return;
                }
                if (prioritySection.getCombo() != null && prioritySection.getCombo().isPopupVisible()) {
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

    public Runnable save(final TestCaseDto dto, final BiConsumer<TestCaseDto, CodeGenerator> onSave, final JBPopup[] popupWrapper) {
        return () -> {
            getAllSections().forEach(section -> section.applyTo(dto));

            String title = dto.getDescription();
            if (DescriptionSection.getWrapper().getParent() == null || !title.trim().isEmpty()) {
                onSave.accept(dto, codeGenerator);

                if (popupWrapper[0] != null)
                    popupWrapper[0].closeOk(null);

            } else
                DescriptionSection.setError(true);
        };
    }

    // todo, move to separate class
    public interface IUIAction {
        void execute();
    }
}