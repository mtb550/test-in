package testGit.ui.single.nnew;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class BaseCreateTestCase {
    protected final TitleSection titleSection;
    protected final ExpectedSection expectedSection;
    protected final PrioritySection prioritySection;
    protected final GroupsSection groupsSection;
    protected final StepsSection stepsSection;
    protected final StatusBarSection statusBarSection;

    public BaseCreateTestCase() {
        this.titleSection = new TitleSection();
        this.expectedSection = new ExpectedSection();
        this.prioritySection = new PrioritySection();
        this.groupsSection = new GroupsSection();
        this.stepsSection = new StepsSection();
        this.statusBarSection = new StatusBarSection();
    }

    public void registerShortcut(final JComponent component, final CustomShortcutSet shortcutSet, final UIAction action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                action.execute();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                if (e.getProject() != null && LookupManager.getInstance(e.getProject()).getActiveLookup() != null) {
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