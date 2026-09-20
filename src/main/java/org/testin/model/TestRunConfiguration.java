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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import org.testin.model.markers.TestRunMarker;

import javax.swing.*;
import org.testin.model.markers.DetailRow;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum TestRunConfiguration {
    TEST_TYPE(
            Bundle.message("config.test.type"),
            AllIcons.Nodes.Type,
            new String[]{"", "Functional Test", "Performance Test"},
            ShownWhen.ALWAYS
    ),

    CHANGE_LOG(
            Bundle.message("config.change.log"),
            AllIcons.Nodes.Type,
            Free.OPTIONS,
            ShownWhen.ALWAYS
    ),

    COMMIT_ID(
            Bundle.message("config.commit.id"),
            AllIcons.Nodes.Type,
            Free.OPTIONS,
            ShownWhen.ALWAYS
    ),

    PLATFORM(
            Bundle.message("config.platform"),
            AllIcons.Nodes.PpLib,
            new String[]{"", Answer.WEB, Answer.MOBILE},
            ShownWhen.ALWAYS
    ),

    COMPONENT(
            Bundle.message("config.component"),
            AllIcons.Nodes.PpLib,
            new String[]{"", Answer.FRONTEND, "Backend"},
            ShownWhen.ALWAYS
    ),

    LANGUAGE(
            Bundle.message("config.language"),
            AllIcons.Nodes.Lambda,
            new String[]{"", "English", "Arabic", "French"},
            ShownWhen.ALWAYS
    ),

    BROWSER(
            Bundle.message("config.browser"),
            AllIcons.Nodes.WebFolder,
            new String[]{"", "Chrome", "Firefox", "Safari", "Edge"},
            chosen -> chosen.is(PLATFORM, Answer.WEB) && chosen.is(COMPONENT, Answer.FRONTEND)
    ),

    DEVICE_TYPE(
            Bundle.message("config.device.type"),
            AllIcons.Nodes.Include,
            new String[]{"", "iPhone", "Samsung", "Huawei"},
            chosen -> chosen.is(PLATFORM, Answer.MOBILE) && chosen.is(COMPONENT, Answer.FRONTEND)
    );

    private final @NotNull String displayName;
    private final @NotNull Icon icon;

    private static final class Free {
        private static final String @NotNull[] OPTIONS = new String[0];
    }

    private static final class Answer {
        private static final @NotNull String WEB = "Web";
        private static final @NotNull String MOBILE = "Mobile";
        private static final @NotNull String FRONTEND = "Frontend";
    }

    private final @NotNull String[] options;

    @Getter(AccessLevel.NONE)
    private final @NotNull ShownWhen shownWhen;

    public @NotNull String valueIn(final @NotNull TestRunMarker run) {
        return run.getConfiguration().getOrDefault(this, "");
    }

    public static @NotNull List<DetailRow> rowsOf(final @NotNull TestRunMarker run) {
        return Arrays.stream(values())
                .map(field -> new DetailRow(field.displayName, field.valueIn(run)))
                .toList();
    }

    public static @NotNull Map<TestRunConfiguration, String> answered(final @NotNull Map<TestRunConfiguration, String> chosen) {
        final @NotNull Map<TestRunConfiguration, String> stored = new EnumMap<>(TestRunConfiguration.class);

        for (final TestRunConfiguration field : values()) {
            final @NotNull String value = Objects.toString(chosen.get(field), "");
            if (!value.isEmpty()) stored.put(field, value);
        }

        return stored;
    }

    public boolean isChoice() {
        return options.length > 0;
    }

    public boolean isShownFor(final @NotNull Chosen chosen) {
        return shownWhen.holds(chosen);
    }

    @FunctionalInterface
    public interface ShownWhen {
        @NotNull ShownWhen ALWAYS = chosen -> true;

        boolean holds(final @NotNull Chosen chosen);
    }

    @FunctionalInterface
    public interface Chosen {
        @NotNull String in(final @NotNull TestRunConfiguration field);

        default boolean is(final @NotNull TestRunConfiguration field, final @NotNull String answer) {
            return answer.equals(in(field));
        }
    }
}
