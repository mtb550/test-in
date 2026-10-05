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


package org.testin.help;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

@Getter
@AllArgsConstructor
public enum Guide {
    SET_UP_THIS_MACHINE(
            Bundle.message("guide.set.up.this.machine"),
            "guides/setUpThisMachine.md"
    ),

    LINK_THIS_REPOSITORY(
            Bundle.message("guide.link.this.repository"),
            "guides/linkThisRepository.md"
    ),

    RAISE_BUG_REPORTS(
            Bundle.message("guide.raise.bug.reports"),
            "guides/raiseBugReports.md"
    ),

    TEST_CASE_EDITOR_SHORTCUTS(
            Bundle.message("guide.test.case.editor.shortcuts"),
            "guides/testCaseEditorShortcuts.md"
    ),

    TEST_RUN_EDITOR_SHORTCUTS(
            Bundle.message("guide.test.run.editor.shortcuts"),
            "guides/testRunEditorShortcuts.md"
    );

    private final @NotNull String title;

    private final @NotNull String page;

    // UC-INTERNAL-009, Rule-INTERNAL-130
    public void open(final @NotNull Project p) {
        new GuideDialog(p, this).show();
    }
}
