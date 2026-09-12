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

package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Set;

/**
 * UC-EDITOR-PANEL-005.
 * <p>
 * The groups a test case is filed under, one box each, added with CTRL+G.
 * <p>
 * It was a row of tick boxes, one per constant of an enum - so the groups a team
 * could use were the groups somebody shipped, and the row grew wider every time
 * one was added. #200 made all eight visible, which was right and made the shape
 * plain: a list that only grows, drawn as a control that only widens.
 * <p>
 * A group is a word now, completed from every group the project has used, so
 * this is the same section the steps are: type to add one, clear the box to take
 * it off (#296).
 */
public class GroupSection extends AbstractMultiValueSection {

    public GroupSection(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull CreateTestCaseFields field() {
        return CreateTestCaseFields.GROUP;
    }

    @Override
    protected @NotNull Set<String> completions(final @NotNull TestCaseValues cache) {
        return cache.getGroups();
    }

    @Override
    protected @NotNull List<String> valuesOf(final @NotNull TestCaseDto dto) {
        return dto.getGroup();
    }

    @Override
    protected void write(final @NotNull TestCaseDto dto, final @NotNull List<String> values) {
        dto.setGroup(values);
    }

    @Override
    protected @NotNull Shortcuts addKey() {
        return Shortcuts.CreateTestCaseGroup;
    }
}
