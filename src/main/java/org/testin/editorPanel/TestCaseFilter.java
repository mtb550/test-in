package org.testin.editorPanel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.enums.TestStatus;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared, null-safe filtering for test and test-run editors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseFilter {

    public static @NotNull List<TestCaseDto> filter(
            final @NotNull Collection<TestCaseDto> source,
            final @Nullable String query,
            final @NotNull Set<Group> groups,
            final @NotNull Set<Priority> priorities,
            final @NotNull Set<String> modules) {
        return filter(source, query, groups, priorities, modules, Collections.emptySet(), ignored -> null);
    }

    public static @NotNull List<TestCaseDto> filter(
            final @NotNull Collection<TestCaseDto> source,
            final @Nullable String query,
            final @NotNull Set<Group> groups,
            final @NotNull Set<Priority> priorities,
            final @NotNull Set<String> modules,
            final @NotNull Set<TestStatus> statuses,
            final @NotNull Function<UUID, TestRunItems> runItemProvider) {
        if (source.isEmpty()) {
            return Collections.emptyList();
        }

        final String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(testCase -> matches(testCase, normalizedQuery, groups, priorities, modules, statuses, runItemProvider))
                .collect(Collectors.toList());
    }

    private static boolean matches(
            final @NotNull TestCaseDto testCase,
            final @NotNull String query,
            final @NotNull Set<Group> groups,
            final @NotNull Set<Priority> priorities,
            final @NotNull Set<String> modules,
            final @NotNull Set<TestStatus> statuses,
            final @NotNull Function<UUID, TestRunItems> runItemProvider) {
        final boolean matchesSearch = query.isEmpty()
                || containsIgnoreCase(testCase.getDescription(), query)
                || testCase.getId().toString().toLowerCase(Locale.ROOT).contains(query)
                || containsIgnoreCase(testCase.getExpectedResult(), query)
                || testCase.getSteps().stream().anyMatch(step -> containsIgnoreCase(step, query));
        final boolean matchesPriority = priorities.isEmpty() || priorities.contains(testCase.getPriority());
        final boolean matchesGroup = groups.isEmpty()
                || (groups.contains(Group.UNASSIGNED) && testCase.getGroup().isEmpty())
                || testCase.getGroup().stream().anyMatch(groups::contains);
        final boolean matchesModule = modules.isEmpty() || modules.contains(testCase.getModule());
        final boolean matchesStatus = statuses.isEmpty()
                || matchesStatus(testCase.getId(), statuses, runItemProvider);

        return matchesSearch && matchesPriority && matchesGroup && matchesModule && matchesStatus;
    }

    private static boolean matchesStatus(
            final @NotNull UUID id,
            final @NotNull Set<TestStatus> statuses,
            final @NotNull Function<UUID, TestRunItems> runItemProvider) {
        final TestRunItems runItem = runItemProvider.apply(id);
        return runItem != null && statuses.contains(runItem.getStatus());
    }

    private static boolean containsIgnoreCase(final @Nullable String value, final @NotNull String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
