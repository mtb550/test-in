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

import javax.swing.JTree;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunTreeCellRenderer {
    public static @NotNull CheckboxTree.CheckboxTreeCellRenderer create() {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(final @NotNull JTree tree, final @NotNull Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    if (node.getUserObject() instanceof DirectoryDto dir)
                        getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                    else if (node.getUserObject() instanceof TestCaseDto tc)
                        getTextRenderer().append(tc.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        };
    }
}
