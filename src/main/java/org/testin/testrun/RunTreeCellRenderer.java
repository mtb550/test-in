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

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;

import javax.swing.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunTreeCellRenderer {

    public static @NotNull CheckboxTree.CheckboxTreeCellRenderer create() {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            // Both @NotNull because the platform says so, not because it looks
            // right: CheckboxTreeCellRendererBase is Kotlin, and its bytecode
            // calls Intrinsics.checkNotNullParameter on tree and on value. It
            // already throws on null, so the annotations cannot add a crash -
            // which is the check that was missing when an unverified @NotNull on
            // a renderer parameter caused the paint crash fixed in 92f1a1ed.
            @Override
            public void customizeRenderer(final @NotNull JTree tree, final @NotNull Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    // instanceof is false for a node carrying nothing, so the kinds
                    // below answer for the empty node too.
                    final @NotNull Object userObj = node.getUserObject();

                    if (userObj instanceof DirectoryDto dir)
                        getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                    // A case is drawn plainly: it is in the tree to be picked,
                    // not to report a verdict.
                    else if (userObj instanceof TestCaseDto tc)
                        getTextRenderer().append(tc.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                    else if (userObj instanceof String str)
                        getTextRenderer().append(str, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }
        };
    }
}
