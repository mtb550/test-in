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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Group;
import testGit.pojo.Priority;
import testGit.pojo.TestEditorAttributes;
import testGit.util.IconManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FilterPopup extends AbstractButton implements IToolbarItem {
    @Getter
    private final Set<Group> selectedGroup = new HashSet<>();

    @Getter
    private final Set<Priority> selectedPriority = new HashSet<>();

    private final DefaultActionGroup cachedActionGroup;

    private final Runnable onToolBarFilterResetted;

    public FilterPopup(final Runnable onToolBarFilterResetted, final Runnable onToolBarFilterSelectedChanged) {
        super("Filter", AllIcons.General.Filter);
        this.onToolBarFilterResetted = onToolBarFilterResetted;

        this.cachedActionGroup = buildActionGroup(onToolBarFilterSelectedChanged);

        addActionListener(e -> showFilterPopup());
        updateToolBarFilterState();
    }

    public void updateToolBarFilterState() {
        int activeFiltersCount = selectedPriority.size() + selectedGroup.size();
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

    public void resetToolBarFilter() {
        selectedPriority.clear();
        selectedGroup.clear();
        updateToolBarFilterState();
        if (onToolBarFilterResetted != null) {
            onToolBarFilterResetted.run();
        }
    }

    private DefaultActionGroup buildActionGroup(final Runnable onToolBarFilterSelectedChanged) {
        DefaultActionGroup filterResetBtn = new DefaultActionGroup();

        filterResetBtn.add(new DumbAwareAction("Reset Filters", "Clear active filters", AllIcons.Actions.Cancel) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                boolean hasActiveFilters = !selectedPriority.isEmpty() || !selectedGroup.isEmpty();
                e.getPresentation().setEnabledAndVisible(hasActiveFilters);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                resetToolBarFilter();
            }
        });
        filterResetBtn.addSeparator();

        // priority menu
        DefaultActionGroup filterPriorityMenu = new DefaultActionGroup(TestEditorAttributes.PRIORITY.getName(), true);
        Arrays.stream(Priority.values()).forEach(p ->
                filterPriorityMenu.add(new DumbAwareToggleAction(p.getName(), null, IconManager.createIcon(p.getColor())) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return selectedPriority.contains(p);
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        p.onChange(selectedPriority, state);
                        updateToolBarFilterState();
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
        DefaultActionGroup filterGroupMenu = new DefaultActionGroup(TestEditorAttributes.GROUP.getName(), true);
        Arrays.stream(Group.values()).forEach(g ->
                filterGroupMenu.add(new DumbAwareToggleAction(g.getName()) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return selectedGroup.contains(g);
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        g.onChange(selectedGroup, state);
                        updateToolBarFilterState();
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

        return filterResetBtn;
    }

    private void showFilterPopup() {
        JBPopupFactory.getInstance()
                .createActionGroupPopup(null, cachedActionGroup,
                        DataManager.getInstance().getDataContext(this),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true)
                .showUnderneathOf(this);
    }
}