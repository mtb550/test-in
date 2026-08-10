package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.enums.TestEditorAttributes;
import org.testin.enums.TestStatus;
import org.testin.util.IconManager;

import java.util.*;
import java.util.function.Supplier;

public class FilterPopupBtn extends AbstractButton implements IToolbarItem {
    @Getter
    @NotNull
    private final Set<Group> selectedGroup = new HashSet<>();

    @Getter
    @NotNull
    private final Set<Priority> selectedPriority = new HashSet<>();

    @Getter
    @NotNull
    private final Set<String> selectedModule = new HashSet<>();

    @Getter
    @NotNull
    private final Set<TestStatus> selectedStatus = new HashSet<>();

    @NotNull
    private final Supplier<Set<String>> availableModulesSupplier;

    @NotNull
    private final DefaultActionGroup cachedActionGroup;

    @NotNull
    private final Runnable onToolBarFilterReset;

    @NotNull
    private final IToolBar callbacks;

    public FilterPopupBtn(final @NotNull IToolBar callbacks, final @NotNull Runnable onToolBarFilterReset, final Runnable onToolBarFilterSelectedChanged, final @NotNull Supplier<Set<String>> availableModulesSupplier) {
        super("Filter", AllIcons.General.Filter);
        this.callbacks = callbacks;
        this.onToolBarFilterReset = onToolBarFilterReset;

        this.availableModulesSupplier = availableModulesSupplier;

        this.cachedActionGroup = buildActionGroup(onToolBarFilterSelectedChanged);

        addActionListener(e -> showFilterPopup());
        updateToolBarFilterState();
    }

    public void updateToolBarFilterState() {
        int activeFiltersCount = selectedPriority.size() + selectedGroup.size() + selectedModule.size() + selectedStatus.size();
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
        clearFilters();
        onToolBarFilterReset.run();
    }

    /**
     * Clears the UI state without triggering a second editor refresh.
     */
    public void clearFilters() {
        selectedPriority.clear();
        selectedGroup.clear();
        selectedModule.clear();
        selectedStatus.clear();
        updateToolBarFilterState();
    }

    private DefaultActionGroup buildActionGroup(final Runnable onToolBarFilterSelectedChanged) {
        DefaultActionGroup filterResetBtn = new DefaultActionGroup();

        filterResetBtn.add(new DumbAwareAction("Reset Filters", "Clear active filters", AllIcons.Actions.Cancel) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                boolean hasActiveFilters = !selectedPriority.isEmpty() || !selectedGroup.isEmpty() || !selectedModule.isEmpty() || !selectedStatus.isEmpty();
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
        Arrays.stream(Group.values()).forEach(g -> {
            if (g == Group.REGRESSION) {
                filterGroupMenu.addSeparator();
            }

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
            });
        });
        filterResetBtn.add(filterGroupMenu);

        ActionGroup filterModuleMenu = new ActionGroup("Module", true) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                List<AnAction> actions = new ArrayList<>();

                Set<String> modules = availableModulesSupplier.get();

                if (!modules.isEmpty()) {
                    List<String> sortedModules = new ArrayList<>(modules);
                    Collections.sort(sortedModules);

                    for (String module : sortedModules) {
                        actions.add(new DumbAwareToggleAction(module) {
                            @Override
                            public boolean isSelected(@NotNull AnActionEvent e) {
                                return selectedModule.contains(module);
                            }

                            @Override
                            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                                if (state) {
                                    selectedModule.add(module);
                                } else {
                                    selectedModule.remove(module);
                                }
                                updateToolBarFilterState();
                                if (onToolBarFilterSelectedChanged != null) {
                                    onToolBarFilterSelectedChanged.run();
                                }
                            }

                            @Override
                            public @NotNull ActionUpdateThread getActionUpdateThread() {
                                return ActionUpdateThread.BGT;
                            }
                        });
                    }
                }
                return actions.toArray(new AnAction[0]);
            }
        };
        filterResetBtn.add(filterModuleMenu);

        if (callbacks instanceof RunEditor) {
            DefaultActionGroup filterStatusMenu = new DefaultActionGroup("Status", true);
            Arrays.stream(TestStatus.values()).forEach(s ->
                    filterStatusMenu.add(new DumbAwareToggleAction(s.name()) {
                        @Override
                        public boolean isSelected(@NotNull AnActionEvent e) {
                            return selectedStatus.contains(s);
                        }

                        @Override
                        public void setSelected(@NotNull AnActionEvent e, boolean state) {
                            if (state) {
                                selectedStatus.add(s);
                            } else {
                                selectedStatus.remove(s);
                            }
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
            filterResetBtn.add(filterStatusMenu);
        }

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
