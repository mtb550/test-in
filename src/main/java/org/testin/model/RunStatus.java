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

package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import org.testin.util.Bundle;
import javax.swing.*;
import java.awt.*;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            AllIcons.RunConfigurations.TestState.Run,
            Badge.NONE,
            Optional.empty()
    ),

    PASSED(
            AllIcons.RunConfigurations.TestPassed,
            new Badge(TestStatus.PASSED.getLabel(), new JBColor(new Color(100, 200, 100), new Color(50, 150, 50))),
            Optional.of(TestStatus.PASSED)
    ),

    FAILED(
            AllIcons.RunConfigurations.TestFailed,
            new Badge(TestStatus.FAILED.getLabel(), new JBColor(new Color(255, 100, 100), new Color(180, 50, 50))),
            Optional.of(TestStatus.FAILED)
    ),

    RUNNING(
            AllIcons.Actions.Suspend,
            new Badge(Bundle.message("run.status.running"), new JBColor(new Color(255, 200, 100), new Color(200, 150, 50))),
            Optional.empty()
    );

    private final @NotNull Icon icon;

    private final @NotNull Badge badge;

    private final @NotNull Optional<TestStatus> verdict;

    public boolean stillGoing() {
        return this == RUNNING;
    }

    public boolean hasBadge() {
        return badge != Badge.NONE;
    }

    public record Badge(@NotNull String label, @NotNull JBColor color) {
        public static final @NotNull Badge NONE = new Badge("", JBColor.GRAY);
    }
}
