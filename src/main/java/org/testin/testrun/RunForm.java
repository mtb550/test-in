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

package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckedTreeNode;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunConfiguration;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.ui.framework.SelectionTree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
public final class RunForm {
    private final @NotNull Project p;

    public static @NotNull Set<UUID> checkedCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> ids = new LinkedHashSet<>();

        selection.forEachChecked(checked -> {
            if (checked instanceof TestCaseDto tc) ids.add(tc.getId());
        });

        return ids;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-076
    public static @NotNull Set<UUID> offeredCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> ids = new LinkedHashSet<>();

        selection.forEachLeaf(leaf -> {
            if (leaf instanceof TestCaseDto tc) ids.add(tc.getId());
        });

        return ids;
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, UC-TREE-PANEL-022
    public void open(final @NotNull DirectoryDto testCasesRoot, final @NotNull String name, final @NotNull Set<UUID> checked, final @NotNull Map<TestRunConfiguration, String> configuration, final @NotNull RunFormAction action) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesRoot.getPath(), testCasesRoot);

            final @NotNull CheckedTreeNode root = convertToCheckedNodes(fullModelNode);
            if (!checked.isEmpty()) checkOnly(root, checked);

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull RunConfigurationForm form = new RunConfigurationForm(name);
                if (!configuration.isEmpty()) form.fillFrom(configuration);

                final @NotNull SelectionTree selection = new SelectionTree(root, RunTreeCellRenderer.create());

                new RunConfigurationDialog(p, form, selection, action).show();
            });
        });
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-093, Rule-TREE-PANEL-076
    private boolean checkOnly(final @NotNull CheckedTreeNode node, final @NotNull Set<UUID> wanted) {
        if (node.getUserObject() instanceof TestCaseDto tc) {
            final boolean covered = wanted.contains(tc.getId());
            node.setChecked(covered);

            return covered;
        }

        boolean allCovered = node.getChildCount() > 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            allCovered &= checkOnly((CheckedTreeNode) node.getChildAt(i), wanted);
        }

        node.setChecked(allCovered);

        return allCovered;
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-030
    private @NotNull DefaultMutableTreeNode buildDirectoryTree(final @NotNull Path folder, final @NotNull DirectoryDto thisNodeDto) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        indexer.awaitIndexing();

        final @NotNull DefaultMutableTreeNode node = new DefaultMutableTreeNode(thisNodeDto);

        if (thisNodeDto instanceof TestSetDirectoryDto) {
            for (final TestCaseDto tc : indexer.getTestCasesForTestSet(folder)) {
                node.add(new DefaultMutableTreeNode(tc));
            }
            return node;
        }

        for (final DirectoryDto child : indexer.getChildren(folder)) {
            if (child.isRetired()) continue;

            final @NotNull DefaultMutableTreeNode childNode = buildDirectoryTree(child.getPath(), child);

            if (childNode.getChildCount() > 0) node.add(childNode);
        }

        return node;
    }

    private @NotNull CheckedTreeNode convertToCheckedNodes(final @NotNull DefaultMutableTreeNode node) {
        final @NotNull Object userObj = node.getUserObject();
        final @NotNull CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }
}
