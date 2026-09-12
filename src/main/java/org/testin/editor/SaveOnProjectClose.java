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

package org.testin.editor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;


public final class SaveOnProjectClose implements ProjectCloseListener {

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-015
    @Override
    public void projectClosingBeforeSave(final @NotNull Project p) {
        // Written down first, then closed - in that order, because closing is
        // what makes there be nothing left to write down. Both halves belong
        // here rather than one of them inside the other: this listener is the
        // only thing that knows the IDE is about to save its tab list, and the
        // two halves are now two classes - what was open is remembered by
        // LastOpenEditors, and closing is what TestinEditors does (#291).
        Services.getInstance(p, LastOpenEditors.class).remember(p);
        Services.getInstance(p, TestinEditors.class).closeAll(p);
    }

}
