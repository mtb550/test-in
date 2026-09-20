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
import org.jetbrains.annotations.Nullable;
import org.testin.model.DirectoryType;
import org.testin.model.markers.Marker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString()
public abstract class DirectoryDto {
    @NonNull
    @Builder.Default
    private String name = "";

    @NonNull
    @Builder.Default
    private Path path = Path.of("");

    @NonNull
    @Builder.Default
    private ArrayList<String> path2 = new ArrayList<>();

    @ToString.Exclude
    private @Nullable DirectoryDto parent;

    public @NotNull List<DirectoryDto> selfAndAncestors() {
        final @NotNull List<DirectoryDto> chain = new ArrayList<>();
        for (DirectoryDto current = this; current != null; current = current.getParent()) {
            chain.add(current);
        }
        return chain;
    }

    public static @NotNull ArrayList<String> pathOf(final @NotNull List<String> parentPath, final @NotNull String name) {
        final @NotNull ArrayList<String> path = new ArrayList<>(parentPath);
        path.add(name);

        return path;
    }

    @NonNull
    public abstract Marker getMarker();

    public int getOrder() {
        return getMarker().getOrder();
    }

    public boolean isOrderable() {
        return false;
    }

    @NonNull
    public String getMarkerFileName() {
        return getType().getMarker();
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-009
    public @NotNull List<DirectoryType> childKinds() {
        return List.of();
    }

    public boolean canCreateChildren() {
        return !childKinds().isEmpty();
    }

    public boolean isRenamable() {
        return true;
    }

    // Rule-TREE-PANEL-100
    public @NotNull List<DirectoryDto> fixedChildren() {
        return List.of();
    }

    public boolean isTestCaseContainer() {
        return false;
    }

    public boolean isOpenableInEditor() {
        return false;
    }

    public boolean isTransferable() {
        return true;
    }

    public boolean isRemovable() {
        return true;
    }

    public boolean isTransferTarget() {
        return getType().acceptsAnything();
    }

    public boolean acceptsTransferred(final @NotNull DirectoryDto source) {
        return getType().accepts(source.getType());
    }

    public boolean isRetired() {
        return false;
    }

    @NotNull
    public abstract DirectoryType getType();
}
