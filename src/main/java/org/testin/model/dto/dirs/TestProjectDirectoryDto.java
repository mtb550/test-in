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

package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.markers.TestProjectMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TestProjectDirectoryDto extends DirectoryDto {

    @NotNull
    @Builder.Default
    private TestCasesMainDirectoryDto testCasesDirectory = new TestCasesMainDirectoryDto();

    @NotNull
    @Builder.Default
    private TestRunsMainDirectoryDto testRunsDirectory = new TestRunsMainDirectoryDto();


    @NotNull
    @Builder.Default
    private TestProjectMarker marker = new TestProjectMarker();



    // The test project node is fixed: not renamed, moved, removed or pasted
    // into from the tree - it is managed through its own actions.

    @Override
    public boolean isRenamable() {
        return false;
    }

    @Override
    public boolean isTransferable() {
        return false;
    }


    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TP;
    }
}
