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
import org.testin.model.PackageStatus;
import org.testin.model.markers.TestSetPackageMarker;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestSetPackageDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestSetPackageMarker marker = new TestSetPackageMarker();

    @Override
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TSP;
    }

    @Override
    public @NotNull List<DirectoryType> childKinds() {
        return DirectoryType.UNDER_TEST_CASES;
    }

    @Override
    public boolean isRetired() {
        return marker.getStatus() == PackageStatus.ARCHIVED;
    }

    @Override
    public boolean isOrderable() {
        return true;
    }
}
