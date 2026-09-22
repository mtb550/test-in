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

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.markers.TestRunMarker;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunDirectoryDto extends DirectoryDto {
    private static final @NotNull String SCREENSHOT_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int SCREENSHOT_NAME_LENGTH = 5;
    private static final @NotNull Pattern SCREENSHOT_NAME = Pattern.compile("[0-9a-z]{" + SCREENSHOT_NAME_LENGTH + "}\\.png");
    @NotNull
    @Builder.Default
    private TestRunMarker marker = new TestRunMarker();

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public static @NotNull String newScreenshotName(final @NotNull Set<String> taken) {
        return Stream.generate(TestRunDirectoryDto::randomScreenshotName)
                .filter(name -> !taken.contains(name))
                .findFirst()
                .orElseThrow();
    }

    private static @NotNull String randomScreenshotName() {
        return ThreadLocalRandom.current().ints(SCREENSHOT_NAME_LENGTH, 0, SCREENSHOT_CHARACTERS.length())
                .mapToObj(index -> String.valueOf(SCREENSHOT_CHARACTERS.charAt(index)))
                .collect(Collectors.joining()) + ".png";
    }

    public static boolean isScreenshotName(final @NotNull String fileName) {
        return SCREENSHOT_NAME.matcher(fileName).matches();
    }

    public static @NotNull Path screenshotFile(final @NotNull Path runPath, final @NotNull String name) {
        return runPath.resolve(name);
    }

    @Override
    public boolean isOpenableInEditor() {
        return true;
    }

    public boolean isStillOpen() {
        return !marker.getStatus().isTerminal();
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TR;
    }

    @Override
    public boolean isOrderable() {
        return isStillOpen();
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-009
    @Override
    public boolean isRenamable() {
        return isStillOpen();
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-094, Rule-TREE-PANEL-009
    @Override
    public boolean isRemovable() {
        return true;
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-009
    @Override
    public boolean isTransferable() {
        return isStillOpen();
    }
}
