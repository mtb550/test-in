package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.toolBar.ToolBarSettings;
import testGit.pojo.Group;
import testGit.pojo.Priority;
import testGit.pojo.TestCaseAttributes;
import testGit.util.IconManager;

import java.util.Arrays;
import java.util.Set;

public class FilterPopup extends AbstractButton implements IToolbarItem {
    private final ToolBarSettings settings;

    // TODO: change both to consumer
    public FilterPopup(final ToolBarSettings settings, final Runnable onReset, final Runnable onFilterChanged) {
        super("Filter", AllIcons.General.Filter);
        this.settings = settings;

        addActionListener(e -> showFilterPopup(onReset, onFilterChanged));

        updateState();
    }

    public void updateState() {
        int activeFiltersCount = settings.getSelectedPriority().size() + settings.getSelectedGroup().size();
        if (activeFiltersCount == 0) {
            setText(null);
            setToolTipText("Filter");
            setForeground(JBColor.foreground());
        } else {
            setText("(" + activeFiltersCount + ")");
            setToolTipText("Filter [" + activeFiltersCount + " active]");
            setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        }
    }

    private void showFilterPopup(final Runnable onToolBarFilterResetted, final Runnable onToolBarFilterSelectedChanged) {
        Set<Priority> selectedPriorities = settings.getSelectedPriority();
        Set<Group> selectedGroups = settings.getSelectedGroup();

        DefaultActionGroup filterResetBtn = new DefaultActionGroup();

        filterResetBtn.add(new DumbAwareAction("Reset Filters", "Clear active filters", AllIcons.Actions.Cancel) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                boolean hasActiveFilters = !selectedPriorities.isEmpty() || !selectedGroups.isEmpty();
                e.getPresentation().setEnabledAndVisible(hasActiveFilters);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (onToolBarFilterResetted != null) {
                    onToolBarFilterResetted.run();
                }

            }

        });
        filterResetBtn.addSeparator();

        // priority menu
        DefaultActionGroup filterPriorityMenu = new DefaultActionGroup(TestCaseAttributes.PRIORITY.getName(), true);
        Arrays.stream(Priority.values()).forEach(p ->
                filterPriorityMenu.add(new DumbAwareToggleAction(p.getName(), null, IconManager.createIcon(p.getColor())) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return selectedPriorities.contains(p);
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        p.onChange(selectedPriorities, state);
                        updateState();
                        if (onToolBarFilterSelectedChanged != null) {
                            onToolBarFilterSelectedChanged.run();
                        }
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.BGT;
                    }
                }));
        filterResetBtn.add(filterPriorityMenu);

        // group menu
        DefaultActionGroup filterGroupMenu = new DefaultActionGroup(TestCaseAttributes.GROUP.getName(), true);
        Arrays.stream(Group.values()).forEach(g ->
                filterGroupMenu.add(new DumbAwareToggleAction(g.getName()) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return selectedGroups.contains(g);
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        g.onChange(selectedGroups, state);
                        updateState();
                        if (onToolBarFilterSelectedChanged != null) {
                            onToolBarFilterSelectedChanged.run();
                        }
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.BGT;
                    }
                })
        );
        filterResetBtn.add(filterGroupMenu);

        JBPopupFactory.getInstance()
                .createActionGroupPopup(null, filterResetBtn,
                        DataManager.getInstance().getDataContext(this),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true)
                .showUnderneathOf(this);
    }
}