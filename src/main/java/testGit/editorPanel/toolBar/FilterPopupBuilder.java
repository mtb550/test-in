package testGit.editorPanel.toolBar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.TestCaseAttributes;

import javax.swing.*;
import java.util.Set;

public class FilterPopupBuilder {
    /// to be implemented, change Runnable to Consumer
    public static void showDetailsPopup(final JButton anchor, final Set<String> selectedDetails, final Set<Groups> selectedGroups, final Runnable onChange) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        ///  to be implemented, standardOptions to be in enum @{@link TestCaseAttributes}
        TestCaseAttributes[] standardOptions = {
                TestCaseAttributes.ID, TestCaseAttributes.MODULE,
                TestCaseAttributes.EXPECTED_RESULT, TestCaseAttributes.STEPS,
                TestCaseAttributes.AUTO_REF, TestCaseAttributes.BUSI_REF
        };

        for (TestCaseAttributes option : standardOptions) {
            actionGroup.add(createCheckboxAction(option.getDisplayName(), option.name(), null, selectedDetails, onChange));
        }

        actionGroup.addSeparator();

        DefaultActionGroup priorityMenu = new DefaultActionGroup(TestCaseAttributes.PRIORITY.getDisplayName(), true);
        priorityMenu.add(createCheckboxAction("Show " + TestCaseAttributes.PRIORITY.getDisplayName() + " Badge",
                TestCaseAttributes.PRIORITY.name(), null, selectedDetails, onChange));
        priorityMenu.addSeparator();

        for (Priority p : Priority.values()) {
            String displayName = p.name().charAt(0) + p.name().substring(1).toLowerCase();
            priorityMenu.add(createCheckboxAction(displayName, p.name(), p.getIcon(), selectedDetails, onChange));
        }

        actionGroup.add(priorityMenu);

        DefaultActionGroup groupsMenu = new DefaultActionGroup(TestCaseAttributes.GROUPS.getDisplayName(), true);
        groupsMenu.add(createCheckboxAction("Show " + TestCaseAttributes.GROUPS.getDisplayName() + " Badge",
                TestCaseAttributes.GROUPS.name(), null, selectedDetails, onChange));
        groupsMenu.addSeparator();

        for (Groups g : Groups.values()) {
            groupsMenu.add(new CheckboxAction(g.name()) {
                @Override
                public boolean isSelected(@NotNull AnActionEvent e) {
                    return selectedGroups.contains(g);
                }

                @Override
                public void setSelected(@NotNull AnActionEvent e, boolean state) {
                    if (state) selectedGroups.add(g);
                    else selectedGroups.remove(g);
                    if (onChange != null) onChange.run();
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }
            });
        }
        actionGroup.add(groupsMenu);

        showActionPopup(anchor, actionGroup);
    }

    private static CheckboxAction createCheckboxAction(final String title, final String key, final Icon icon, final Set<String> selectedDetails, final Runnable onChange) {
        CheckboxAction action = new CheckboxAction(title) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return selectedDetails.contains(key);
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (state) selectedDetails.add(key);
                else selectedDetails.remove(key);
                if (onChange != null) onChange.run();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };

        /// here put the icon or color for priority. retrieve it from enum.
        if (icon != null) {
            action.getTemplatePresentation().setIcon(icon);
        }

        return action;
    }

    private static void showActionPopup(final JButton anchor, final DefaultActionGroup actionGroup) {
        JBPopupFactory.getInstance()
                .createActionGroupPopup(
                        null,
                        actionGroup,
                        DataManager.getInstance().getDataContext(anchor),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        false
                ).showUnderneathOf(anchor);
    }
}