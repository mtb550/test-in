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
import org.testin.model.markers.TestRunPackageMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunPackageDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestRunPackageMarker marker = new TestRunPackageMarker();






    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TRP;
    }

    @Override
    public boolean canCreateChildren() {
        return true;
    }

    @Override
    public boolean isRetired() {
        return marker.getStatus() == PackageStatus.ARCHIVED;
    }

    /**
     * A package of test runs is arranged by the tester when they say so. Unnumbered it reads by
     * the date it was created, which is the order runs have always had - the
     * number is for the cycle somebody wants at the top.
     */
    @Override
    public boolean isOrderable() {
        return true;
    }
}
