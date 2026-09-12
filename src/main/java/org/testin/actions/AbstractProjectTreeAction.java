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

package org.testin.actions;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * An action on the project tree: it needs the project and the tree, and twelve
 * of them declared both for themselves.
 * <p>
 * Only for actions whose tree is always there. {@code EscapeAction} and
 * {@code GenerateReportAction} have constructors that take a list or a table
 * instead, and each of those says what the action does rather than which
 * surface it was given, so neither keeps a tree at all - both extend
 * {@link AbstractProjectAction} directly.
 * <p>
 * Like its parent, this holds constructor arguments and nothing else.
 */
public abstract class AbstractProjectTreeAction extends AbstractProjectAction {

    protected final @NotNull SimpleTree tree;

    protected AbstractProjectTreeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull String title, final @NotNull String description, final @NotNull Icon icon) {
        super(p, title, description, icon);
        this.tree = tree;
    }
}
