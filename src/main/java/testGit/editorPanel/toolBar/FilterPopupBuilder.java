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
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class FilterPopupBuilder {
    public static void showDetailsPopup(final JButton anchor, final Set<String> selectedDetails, final Set<Groups> selectedGroups, final Consumer<Void> onChange) {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();

        Arrays.stream(TestCaseAttributes.values())
                .filter(TestCaseAttributes::isStandardOption)
                .forEach(option -> actionGroup.add(createCheckboxAction(option.getDisplayName(), option.name(), null, selectedDetails, onChange)));

        actionGroup.addSeparator();

        final DefaultActionGroup priorityMenu = new DefaultActionGroup(TestCaseAttributes.PRIORITY.getDisplayName(), true);
        priorityMenu.add(createCheckboxAction("Show " + TestCaseAttributes.PRIORITY.getDisplayName() + " Badge",
                TestCaseAttributes.PRIORITY.name(), null, selectedDetails, onChange));
        priorityMenu.addSeparator();

        Arrays.stream(Priority.values()).forEach(p -> priorityMenu.add(createCheckboxAction(p.getDisplayName(), p.name(), p.getIcon(), selectedDetails, onChange)));

        actionGroup.add(priorityMenu);

        final DefaultActionGroup groupsMenu = new DefaultActionGroup(TestCaseAttributes.GROUPS.getDisplayName(), true);
        groupsMenu.add(createCheckboxAction("Show " + TestCaseAttributes.GROUPS.getDisplayName() + " Badge",
                TestCaseAttributes.GROUPS.name(), null, selectedDetails, onChange));
        groupsMenu.addSeparator();

        Arrays.stream(Groups.values()).forEach(g -> groupsMenu.add(new CheckboxAction(g.getDisplayName()) {
            @Override
            public boolean isSelected(final @NotNull AnActionEvent e) {
                return selectedGroups.contains(g);
            }

            @Override
            public void setSelected(final @NotNull AnActionEvent e, final boolean state) {
                if (state) selectedGroups.add(g);
                else selectedGroups.remove(g);
                Optional.ofNullable(onChange).ifPresent(c -> c.accept(null));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        }));
        actionGroup.add(groupsMenu);

        showActionPopup(anchor, actionGroup);
    }

    private static CheckboxAction createCheckboxAction(final String title, final String key, final Icon icon, final Set<String> selectedDetails, final Consumer<Void> onChange) {
        final CheckboxAction action = new CheckboxAction(title) {
            @Override
            public boolean isSelected(final @NotNull AnActionEvent e) {
                return selectedDetails.contains(key);
            }

            @Override
            public void setSelected(final @NotNull AnActionEvent e, final boolean state) {
                if (state) selectedDetails.add(key);
                else selectedDetails.remove(key);

                Optional.ofNullable(onChange).ifPresent(c -> c.accept(null));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };

        Optional.ofNullable(icon).ifPresent(action.getTemplatePresentation()::setIcon);

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