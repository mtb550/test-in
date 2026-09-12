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

import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.SelectionTree;

/**
 * What the run form's button says, and what pressing it does.
 * <p>
 * Creating a test run and editing one open the same dialog, on the same
 * configuration form and the same selection tree, and differ in exactly this:
 * the words on it, and where the answers go. Holding that difference in one
 * object is what stops editing becoming a second dialog - built by copying the
 * first and then drifting from it, so a question added to creating a run is
 * quietly missing when the same run is edited (#96).
 */
public record RunFormAction(@NotNull String title, @NotNull String button, @NotNull Submit submit) {

    /**
     * Reports whether it happened, rather than being told to happen.
     * <p>
     * A refusal - an empty name, a name a sibling already has - leaves the dialog
     * open with everything the tester typed still in it, which is the whole
     * reason this is not a {@code Runnable} (#9).
     */
    @FunctionalInterface
    public interface Submit {

        boolean of(@NotNull RunConfigurationForm form, @NotNull SelectionTree selection);
    }
}
