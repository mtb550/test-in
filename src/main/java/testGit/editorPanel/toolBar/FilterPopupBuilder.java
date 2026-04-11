package testGit.editorPanel.toolBar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Groups;
import testGit.pojo.Priority;

import javax.swing.*;
import java.util.Set;

public class FilterPopupBuilder {

    public static void showGroupPopup(JButton anchor, Set<Groups> selectedGroups, Runnable onChange) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        for (Groups g : Groups.values()) {
            actionGroup.add(new CheckboxAction(g.name()) {
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
            });
        }

        showActionPopup(anchor, actionGroup);
    }

    public static void showDetailsPopup(JButton anchor, Set<String> selectedDetails, Runnable onChange) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        String[] standardOptions = {"ID", "Module", "Expected Result", "Steps", "Automation Ref", "Business Ref"};
        for (String option : standardOptions) {
            actionGroup.add(createCheckboxAction(option, option, null, selectedDetails, onChange));
        }

        actionGroup.addSeparator();

        DefaultActionGroup priorityMenu = new DefaultActionGroup("Priority", true);
        priorityMenu.add(createCheckboxAction("Show Priority Badge", "Priority", null, selectedDetails, onChange));
        priorityMenu.addSeparator();

        for (Priority p : Priority.values()) {
            String displayName = p.name().charAt(0) + p.name().substring(1).toLowerCase();
            priorityMenu.add(createCheckboxAction(displayName, p.name(), p.getIcon(), selectedDetails, onChange));
        }

        actionGroup.add(priorityMenu);

        showActionPopup(anchor, actionGroup);
    }

    private static CheckboxAction createCheckboxAction(String title, String key, Icon icon, Set<String> selectedDetails, Runnable onChange) {
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
        };

        /// here put the icon or color for priority. retrieve it from enum.
        if (icon != null) {
            action.getTemplatePresentation().setIcon(icon);
        }

        return action;
    }

    private static void showActionPopup(JButton anchor, DefaultActionGroup actionGroup) {
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