package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testCaseEditor.TestEditorUI;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.ui.testCase.TestCaseUpdateMenu;
import org.testin.util.KeyboardSet;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;
import org.testin.util.services.TestCaseCacheService;
import org.testin.util.services.TestCasePersistService;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateTestCase extends DumbAwareAction {

    private final JBList<TestCaseDto> list;
    private final Path path;
    private final IEditorUI ui;

    public UpdateTestCase(final IEditorUI ui, final JBList<TestCaseDto> list, final Path path) {
        super("Edit Test Case");
        this.list = list;
        this.path = path;
        this.ui = ui;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (list == null || project == null) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        Log.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

        new TestCaseUpdateMenu(project, selectedItems, (updatedItems, codeGenerator) -> {

            Services.getInstance(project, TestCaseCacheService.class).addNewItems(updatedItems);
            Services.getInstance(project, TestCasePersistService.class).persist(path, updatedItems);

            Services.getInstance(project, Notifier.class).softShow(project, "Updated..");

            if (ui instanceof IToolBar) {
                ((IToolBar) ui).onToolBarFilterSelectionChanged();
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                list.repaint();

                ViewPanel detailsPanel = ViewToolWindowFactory.getViewPanel();
                if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {
                    boolean isCurrentAffected = updatedItems.stream()
                            .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));

                    if (isCurrentAffected) {
                        detailsPanel.refreshCurrentView();
                    }
                }

                if (codeGenerator != null && codeGenerator.isSelected()) {
                    Log.trace("[UpdateTestCase]: Code generator is selected! Change Type received: " + codeGenerator.getGeneratorType());

                    // todo, all if statements here to be moved to enum class
                    if (codeGenerator.getGeneratorType() == GeneratorType.UPDATE_TEST_CASE_DESCRIPTION) {
                        Log.trace("[UpdateTestCase]: Routing to UpdateTestCaseDescription()...");
                        //new UpdateTestCaseDescription().execute(project, updatedItems.getFirst().getFqcn(), updatedItems.getFirst());
                        return;
                    }

                    if (codeGenerator.getGeneratorType() == GeneratorType.UPDATE_TEST_CASE_EXPECTED_RESULT) {
                        Log.trace("[UpdateTestCase]: Routing to Update Expected Results (Type 2)...");
                        return;
                    }

                    if (codeGenerator.getGeneratorType() == GeneratorType.UPDATE_TEST_CASE_GROUP) {
                        Log.trace("[UpdateTestCase]: Routing to Update Group (Type 2)...");
                        return;
                    }

                    if (codeGenerator.getGeneratorType() == GeneratorType.UPDATE_TEST_CASE_MODULE) {
                        Log.trace("[UpdateTestCase]: Routing to Update Module (Type 2)...");
                        return;
                    }

                    if (codeGenerator.getGeneratorType() == GeneratorType.UPDATE_TEST_CASE_STEPS) {
                        Log.trace("[UpdateTestCase]: Routing to Update Steps (Type 3)...");
                    }

//                    CreateJavaMethodInClass generator = new CreateJavaMethodInClass();
//                    for (TestCaseDto tc : updatedItems) {
//                        generator.execute(project, tc.getFqcn(), tc);
//                    }
                } else {
                    Log.trace("[UpdateTestCase]: Code generator is NOT selected or is null.");
                }

            });
        }).show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ui instanceof TestEditorUI);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}