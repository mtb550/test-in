package org.testin.editor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Group;
import org.testin.model.Priority;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared filtering for the test and test-run editors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseFilter {

    public static @NotNull List<TestCaseDto> filter(final @NotNull Collection<TestCaseDto> source, final @NotNull String query, final @NotNull Set<Group> groups, final @NotNull Set<Priority> priorities, final @NotNull Set<String> modules) {
        // No run items on this path - the test editor has no statuses to filter
        // by. An empty map says that; a function returning null only implies it.
        return filter(source, query, groups, priorities, modules, Collections.emptySet(),
                id -> Optional.empty());
    }

    public static @NotNull List<TestCaseDto> filter(final @NotNull Collection<TestCaseDto> source, final @NotNull String query, final @NotNull Set<Group> groups, final @NotNull Set<Priority> priorities, final @NotNull Set<String> modules, final @NotNull Set<TestStatus> statuses, final @NotNull Function<UUID, Optional<TestRunItems>> runItemProvider) {
        if (source.isEmpty()) {
            return Collections.emptyList();
        }

        final @NotNull String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(testCase -> matches(testCase, normalizedQuery, groups, priorities, modules, statuses, runItemProvider))
                .collect(Collectors.toList());
    }

    private static boolean matches(final @NotNull TestCaseDto testCase, final @NotNull String query, final @NotNull Set<Group> groups, final @NotNull Set<Priority> priorities, final @NotNull Set<String> modules, final @NotNull Set<TestStatus> statuses, final @NotNull Function<UUID, Optional<TestRunItems>> runItemProvider) {
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

    private static boolean matchesStatus(final @NotNull UUID id, final @NotNull Set<TestStatus> statuses, final @NotNull Function<UUID, Optional<TestRunItems>> runItemProvider) {
        return runItemProvider.apply(id)
                .map(TestRunItems::getStatus)
                .filter(statuses::contains)
                .isPresent();
    }

    private static boolean containsIgnoreCase(final @NotNull String value, final @NotNull String query) {
        return value.toLowerCase(Locale.ROOT).contains(query);
    }
}
