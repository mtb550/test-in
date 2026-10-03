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

package org.testin.explorer.tree;

import com.intellij.openapi.application.WriteAction;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.NodeStatus;
import org.testin.model.PackageStatus;
import org.testin.model.TestSetStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RetiredNodesIdeTest extends AbstractTempRootIdeTest {

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private static @NotNull List<String> drawn(final @NotNull DirectoryDto node) {
        final @NotNull TreeCellRenderer renderer = new TreeCellRenderer(Set.of());
        renderer.getTreeCellRendererComponent(new JTree(), new DefaultMutableTreeNode(node), false, false, true, 0, false);

        final @NotNull List<String> fragments = new ArrayList<>();
        final @NotNull SimpleColoredComponent.ColoredIterator each = renderer.iterator();
        while (each.hasNext()) {
            final @NotNull String text = each.next();
            fragments.add(text + (each.getTextAttributes().equals(SimpleTextAttributes.GRAYED_ATTRIBUTES) ? " [grayed]" : ""));
        }
        return fragments;
    }

    private void marked(final @NotNull DirectoryDto node, final @NotNull NodeStatus status) {
        assertTrue("the status was not written", nodes().mark(node, status, "Sara"));
    }

    // Rule-PRODUCT-022
    public void testADeprecatedTestSetIsKeptAndDrawnAsDeprecated() {
        final @NotNull NodesOnDisk onDisk = new NodesOnDisk(getProject());
        final @NotNull TestProjectDirectoryDto tp = onDisk.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetDirectoryDto login = onDisk.testSet(tp.getTestCasesDirectory(), "Login");
        final @NotNull TestCaseDto tc = onDisk.testCase(login);

        marked(login, TestSetStatus.DEPRECATED);

        assertTrue("deprecating the test set deleted its folder", Files.isDirectory(login.getPath()));
        assertTrue("deprecating the test set dropped it from the tree", nodes().find(login.getPath()).isPresent());
        assertTrue("deprecating the test set lost its test cases", Services.getInstance(getProject(), TestCases.class).findTestCase(tc.getId()).isPresent());
        assertEquals("the deprecated test set is not drawn as deprecated", List.of(login.getName() + " [grayed]", " " + TestSetStatus.DEPRECATED.getLabel()), drawn(login));
    }

    // Rule-PRODUCT-023
    public void testActiveAndArchivedMeanTheSameForBothKindsOfPackage() {
        final @NotNull TestProjectDirectoryDto tp = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
        final @NotNull TestSetPackageDirectoryDto accounts = new NodesOnDisk(getProject()).testSetPackage(tp.getTestCasesDirectory(), "Accounts");
        final @NotNull TestRunPackageDirectoryDto release = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunPackageDirectoryDto made = Services.getInstance(getProject(), DirectoryMapper.class).getTestRunPackageNode(tp.getTestRunsDirectory().getPath().resolve("Release"), tp.getTestRunsDirectory());
            nodes().addTestRunPackage(made);
            return made;
        });

        for (final PackageStatus status : PackageStatus.values()) {
            marked(accounts, status);
            marked(release, status);

            assertEquals(status + " sets a test set package and a test run package apart differently", accounts.isRetired(), release.isRetired());
            assertEquals(status + " leaves one kind of package out of Expand All and not the other", new TreePanelNode(getProject(), accounts).isIncludedInExpandAll(), new TreePanelNode(getProject(), release).isIncludedInExpandAll());
            assertEquals(status + " is drawn differently on the two kinds of package", drawn(accounts).stream().map(fragment -> fragment.replace(accounts.getName(), "")).toList(), drawn(release).stream().map(fragment -> fragment.replace(release.getName(), "")).toList());
        }
        assertTrue("an archived package is not set apart at all, so the two agreeing proves nothing", accounts.isRetired());
    }
}
