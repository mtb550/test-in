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

package org.testin.testcase;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenType;
import org.testin.importexport.imports.ImportSetter;
import org.testin.model.Groups;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.NameSanitizer;
import org.testin.util.TestDataParser;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.testin.importexport.imports.ImportSetter.always;
import static org.testin.importexport.imports.ImportSetter.took;

@Getter
public enum TestEditorAttributes implements ToolBarAttribute {
    ORDER(
            Bundle.message("attribute.order"),
            ToolBarDefault.LOCKED_CHECKED,
            _ -> "",
            (_, _, _) -> true,
            GenType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    DESCRIPTION(
            Bundle.message("attribute.description"),
            ToolBarDefault.LOCKED_CHECKED,
            TestCaseDto::getDescription,
            always((tc, v) -> tc.setDescription(NameSanitizer.description(v))),
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    ID(
            Bundle.message("attribute.id"),
            ToolBarDefault.OFF,
            tc -> String.valueOf(tc.getId()),
            (_, _, _) -> true,
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    EXPECTED_RESULT(
            Bundle.message("attribute.expected.result"),
            ToolBarDefault.ON,
            TestCaseDto::getExpectedResult,
            always(TestCaseDto::setExpectedResult),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    STEPS(
            Bundle.message("attribute.steps"),
            ToolBarDefault.OFF,
            tc -> String.join(", ", tc.getSteps()),
            always((tc, v) -> tc.setSteps(TestDataParser.steps(v))),
            GenType.UPDATE_TEST_CASE_STEPS,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    PRIORITY(
            Bundle.message("attribute.priority"),
            ToolBarDefault.ON,
            tc -> tc.getPriority().getLabel(),
            (_, tc, v) -> took(TestDataParser.priority(v, tc.getPriority()), tc::setPriority),
            GenType.UPDATE_TEST_CASE_PRIORITY,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addPriorityBadge(badges, tc);
        }
    },

    FQCN(
            Bundle.message("attribute.fqcn"),
            ToolBarDefault.OFF,
            tc -> String.join(" > ", Fqcn.ofMethod(tc)),
            (_, _, _) -> true,
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    REFERENCE(
            Bundle.message("attribute.reference"),
            ToolBarDefault.OFF,
            TestCaseDto::getReference,
            always(TestCaseDto::setReference),
            GenType.NO_CODE_CHANGE,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    TEST_DATA(
            Bundle.message("attribute.test.data"),
            ToolBarDefault.OFF,
            TestCaseDto::getTestData,
            always(TestCaseDto::setTestData),
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    PRE_CONDITIONS(
            Bundle.message("attribute.pre.conditions"),
            ToolBarDefault.OFF,
            TestCaseDto::getPreConditions,
            always(TestCaseDto::setPreConditions),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    GROUP(
            Bundle.message("attribute.group"),
            ToolBarDefault.ON,
            tc -> Groups.text(tc.getGroup()),
            (_, tc, v) -> took(TestDataParser.groups(v), tc::setGroup),
            GenType.UPDATE_TEST_CASE_GROUP,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
            tc.getGroup().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            Bundle.message("attribute.path"),
            ToolBarDefault.OFF,
            tc -> String.join(" > ", tc.getParent().getPath2()),
            (_, _, _) -> true,
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    MODULE(
            Bundle.message("attribute.module"),
            ToolBarDefault.OFF,
            TestCaseDto::getModule,
            always(TestCaseDto::setModule),
            GenType.UPDATE_TEST_CASE_MODULE,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    STATUS(
            Bundle.message("attribute.status"),
            ToolBarDefault.OFF,
            tc -> tc.getStatus().getLabel(),
            (_, tc, v) -> took(TestDataParser.testCaseStatus(v, tc.getStatus()), tc::setStatus),
            GenType.UPDATE_TEST_CASE_STATUS,
            Can.EDIT, Can.COPY, Can.EXPORT
    ),

    CREATED_BY(
            Bundle.message("attribute.created.by"),
            ToolBarDefault.OFF,
            TestCaseDto::getCreatedBy,
            always(TestCaseDto::setCreatedBy),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATED_BY(
            Bundle.message("attribute.updated.by"),
            ToolBarDefault.OFF,
            TestCaseDto::getUpdatedBy,
            always(TestCaseDto::setUpdatedBy),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    CREATED_AT(
            Bundle.message("attribute.created.at"),
            ToolBarDefault.OFF,
            tc -> Display.formatDate(tc.getCreatedAt()),
            (_, tc, v) -> took(TestDataParser.date(v), tc::setCreatedAt),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATED_AT(
            Bundle.message("attribute.updated.at"),
            ToolBarDefault.OFF,
            tc -> Display.formatDate(tc.getUpdatedAt()),
            (_, tc, v) -> took(TestDataParser.date(v), tc::setUpdatedAt),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    );

    // Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005
    private static final @NotNull Set<TestEditorAttributes> PROSE =
            EnumSet.of(DESCRIPTION, EXPECTED_RESULT, STEPS, PRE_CONDITIONS);
    private static final @NotNull Map<Can, List<TestEditorAttributes>> BY_CAPABILITY = EnumSet.allOf(Can.class).stream()
            .collect(Collectors.toUnmodifiableMap(capability -> capability, capability -> Arrays.stream(values()).filter(attribute -> attribute.can(capability)).toList()));
    private final @NotNull String name;
    private final @NotNull ToolBarDefault toolBarDefault;
    private final @NotNull Function<TestCaseDto, String> testValueExtractor;
    private final @NotNull ImportSetter importSetter;
    private final @NotNull GenType genType;
    @Getter(AccessLevel.NONE)
    private final @NotNull Set<Can> can;

    TestEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault, final @NotNull Function<TestCaseDto, String> testValueExtractor, final @NotNull ImportSetter importSetter, final @NotNull GenType genType, final @NotNull Can... can) {
        this.name = name;
        this.toolBarDefault = toolBarDefault;
        this.testValueExtractor = testValueExtractor;
        this.importSetter = importSetter;
        this.genType = genType;
        this.can = can.length == 0 ? EnumSet.noneOf(Can.class) : EnumSet.copyOf(List.of(can));
    }

    // UC-EDITOR-PANEL-019, UC-INTERNAL-001, Rule-EDITOR-PANEL-091
    public static boolean anyContains(final @NotNull TestCaseDto tc, final @NotNull String wanted) {
        final @NotNull String lowered = wanted.toLowerCase(Locale.ROOT);

        for (final TestEditorAttributes attribute : values()) {
            if (attribute.gridValue(tc).toLowerCase(Locale.ROOT).contains(lowered)) return true;
        }

        return false;
    }

    // UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206
    public static int importRow(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull Function<TestEditorAttributes, String> cell) {
        int refused = 0;

        for (final TestEditorAttributes attr : values()) {
            if (!attr.can(Can.IMPORT)) continue;
            if (!attr.importSetter.execute(p, tc, cell.apply(attr))) refused++;
        }

        return refused;
    }

    // UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206
    public static void sayWhatWasRefused(final @NotNull Project p, final int refused) {
        if (refused == 0) return;

        Services.getInstance(p, Notifier.class).softRefuse(p, Refused.UNREADABLE, refused == 1
                ? Bundle.message("attribute.value.one")
                : Bundle.message("attribute.value.many", String.valueOf(refused)));
    }

    // UC-SHARE-002, Rule-SHARE-001
    public static @NotNull List<TestEditorAttributes> all(final @NotNull Can capability) {
        return BY_CAPABILITY.get(capability);
    }

    // UC-SHARE-005, UC-SHARE-006, Rule-SHARE-110
    public boolean isColumn(final @NotNull String header) {
        final @NotNull String wanted = header.trim();

        return name.equalsIgnoreCase(wanted) || TestDataParser.namesConstant(this, wanted);
    }

    public boolean can(final @NotNull Can capability) {
        return can.contains(capability);
    }

    public @NotNull String gridValue(final @NotNull TestCaseDto tc) {
        return this == STEPS ? String.join("\n", tc.getSteps()) : testValueExtractor.apply(tc);
    }

    // Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005
    public @NotNull String displayValue(final @NotNull TestCaseDto tc) {
        final @NotNull String raw = testValueExtractor.apply(tc);

        return PROSE.contains(this) ? Display.format(raw) : raw;
    }

    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        details.put(name, displayValue(tc));
    }

    public enum Can {
        EDIT,

        IMPORT,

        COPY,

        EXPORT
    }
}
