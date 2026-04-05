package testGit.ui.createTestCase;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Getter
public class CreateTestCaseBase {
    protected final TitleSection titleSection;
    protected final ExpectedSection expectedSection;
    protected final PrioritySection prioritySection;
    protected final GroupsSection groupsSection;
    protected final StepsSection stepsSection;
    protected final StatusBar statusBar;

    public CreateTestCaseBase() {
        this.titleSection = new TitleSection();
        this.expectedSection = new ExpectedSection();
        this.stepsSection = new StepsSection();
        this.prioritySection = new PrioritySection();
        this.groupsSection = new GroupsSection();
        this.statusBar = new StatusBar();
    }

    public List<CreateTestCaseSection> getAllSections() {
        return Arrays.asList(
                titleSection, // arranged, sequence that related to place of each component
                expectedSection,
                stepsSection,
                prioritySection,
                groupsSection
        );
    }

    public void registerShortcut(final JComponent component, final CustomShortcutSet shortcutSet, final UIAction action) {
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

    public interface UIAction {
        void execute();
    }
}