package testGit.editorPanel.toolBar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.TestCaseAttributes;
import testGit.util.IconManager;

import javax.swing.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class FilterPopupBuilder {
    public static void showDetailsPopup(final JButton anchor, final Set<String> selectedDetails, final Consumer<Void> onChange) {
        final CheckBoxList<TestCaseAttributes> list = new CheckBoxList<>();

        Arrays.stream(TestCaseAttributes.values())
                .filter(TestCaseAttributes::isStandardToolBarOption)
                .forEach(attr -> list.addItem(attr, attr.getDisplayName(), selectedDetails.contains(attr.name())));

        list.setCheckBoxListListener((index, state) -> {
            TestCaseAttributes item = list.getItemAt(index);
            if (item != null) {
                if (state) selectedDetails.add(item.name());
                else selectedDetails.remove(item.name());
            }
            Optional.ofNullable(onChange).ifPresent(c -> c.accept(null));
        });

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(list, list)
                .setRequestFocus(true)
                .createPopup()
                .showUnderneathOf(anchor);
    }

    public static void showFilterPopup(final JButton anchor, final Set<Priority> selectedPriorities, final Set<Groups> selectedGroups, final Consumer<Void> onChange) {
        final DefaultActionGroup filterGroup = new DefaultActionGroup();

        final DefaultActionGroup priorityMenu = new DefaultActionGroup(TestCaseAttributes.PRIORITY.getDisplayName(), true);
        Arrays.stream(Priority.values()).forEach(p ->
                priorityMenu.add(new ToggleAction(p.getName(), null, IconManager.createIcon(p.getColor())) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return selectedPriorities.contains(p);
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        p.onChange(selectedPriorities, state);
                        Optional.ofNullable(onChange).ifPresent(c -> c.accept(null));
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.BGT;
                    }
                }));
        filterGroup.add(priorityMenu);

        DefaultActionGroup groupsMenu = new DefaultActionGroup(TestCaseAttributes.GROUPS.getDisplayName(), true);
        Arrays.stream(Groups.values()).forEach(g ->
                groupsMenu.add(new ToggleAction(g.getName()) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return selectedGroups.contains(g);
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        g.onChange(selectedGroups, state);
                        Optional.ofNullable(onChange).ifPresent(c -> c.accept(null));
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.BGT;
                    }
                })
        );
        filterGroup.add(groupsMenu);

        JBPopupFactory.getInstance()
                .createActionGroupPopup(null, filterGroup,
                        DataManager.getInstance().getDataContext(anchor),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        false)
                .showUnderneathOf(anchor);
    }
}