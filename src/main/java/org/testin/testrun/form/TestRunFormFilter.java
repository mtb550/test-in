/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testrun.form;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AutomationState;
import org.testin.filter.FilterPopupBtn;
import org.testin.filter.FilterSelection;
import org.testin.filter.FilterSource;
import org.testin.filter.TestCaseFilter;
import org.testin.model.Modules;
import org.testin.model.TestCaseDto;
import org.testin.model.node.DirectoryDto;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// UC-TREE-PANEL-009, Rule-TREE-PANEL-129
public final class TestRunFormFilter implements FilterSource {
    private final @NotNull Project p;
    private final @NotNull AutomationState automation;
    private final @NotNull List<OfferedTestCase> offered = new ArrayList<>();
    private final @NotNull Map<Path, String> testSets = new LinkedHashMap<>();
    private final @NotNull FilterPopupBtn button;
    private final @NotNull SelectionTree selection;

    public TestRunFormFilter(final @NotNull Project p, final @NotNull Path testCasesRoot, final @NotNull CheckedTreeNode root) {
        this.p = p;
        this.automation = Services.getInstance(p, AutomationState.class);
        collect(root, testCasesRoot);

        this.button = new FilterPopupBtn(this);
        this.selection = new SelectionTree(Bundle.message("test.run.form.test.cases.caption"), root, TestRunTreeCellRenderer.create(), Optional.of(button));

        automation.read(p, testCases(), () -> {
            if (!button.getSelectedAutomation().isEmpty()) onToolBarFilterSelectionChanged();
        });
    }

    private static @NotNull String nameOf(final @NotNull Path testCasesRoot, final @NotNull Path testSet) {
        return testCasesRoot.relativize(testSet).toString().replace(testCasesRoot.getFileSystem().getSeparator(), " / ");
    }

    public @NotNull SelectionTree getSelection() {
        return selection;
    }

    // Rule-TREE-PANEL-129
    @Override
    public @NotNull Map<Path, String> getAvailableTestSets() {
        return testSets;
    }

    @Override
    public @NotNull Set<String> getAvailableModules() {
        return Modules.in(testCases());
    }

    @Override
    public @NotNull Set<String> getAvailableGroups() {
        return Services.getInstance(p, TestCaseValues.class).getGroups();
    }

    // Rule-TREE-PANEL-129, Rule-TREE-PANEL-130
    @Override
    public void onToolBarFilterSelectionChanged() {
        final @NotNull FilterSelection wanted = FilterSelection.of(button, "");
        final @NotNull Set<UUID> matched = automation.matching(TestCaseFilter.filter(testCases(), wanted), wanted.automation()).stream()
                .map(TestCaseDto::getId)
                .collect(Collectors.toSet());

        final @NotNull Set<Path> wantedSets = button.getSelectedTestSet();
        final @NotNull Set<UUID> shown = offered.stream()
                .filter(each -> matched.contains(each.testCase().getId()))
                .filter(each -> wantedSets.isEmpty() || wantedSets.contains(each.testSet()))
                .map(each -> each.testCase().getId())
                .collect(Collectors.toSet());

        selection.show(leaf -> leaf instanceof TestCaseDto tc && shown.contains(tc.getId()));
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
        onToolBarFilterSelectionChanged();
    }

    private @NotNull List<TestCaseDto> testCases() {
        return offered.stream().map(OfferedTestCase::testCase).toList();
    }

    // Rule-TREE-PANEL-129
    private void collect(final @NotNull CheckedTreeNode node, final @NotNull Path testCasesRoot) {
        for (int i = 0; i < node.getChildCount(); i++) {
            final @NotNull CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);

            if (child.getUserObject() instanceof TestCaseDto tc && node.getUserObject() instanceof DirectoryDto testSet) {
                offered.add(new OfferedTestCase(tc, testSet.getPath()));
                testSets.putIfAbsent(testSet.getPath(), nameOf(testCasesRoot, testSet.getPath()));
            } else {
                collect(child, testCasesRoot);
            }
        }
    }
}
