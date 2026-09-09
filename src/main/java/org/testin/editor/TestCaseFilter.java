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

    // UC-EDITOR-PANEL-019, UC-EDITOR-PANEL-020
    public static @NotNull List<TestCaseDto> filter(final @NotNull Collection<TestCaseDto> source, final @NotNull String query, final @NotNull Set<Group> groups, final @NotNull Set<Priority> priorities, final @NotNull Set<String> modules, final @NotNull Set<TestStatus> statuses, final @NotNull Function<UUID, Optional<TestRunItems>> runItemProvider) {
        if (source.isEmpty()) {
            return Collections.emptyList();
        }

        final @NotNull String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(testCase -> matches(testCase, normalizedQuery, groups, priorities, modules, statuses, runItemProvider))
                .collect(Collectors.toList());
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-091
    private static boolean matches(final @NotNull TestCaseDto testCase, final @NotNull String query, final @NotNull Set<Group> groups, final @NotNull Set<Priority> priorities, final @NotNull Set<String> modules, final @NotNull Set<TestStatus> statuses, final @NotNull Function<UUID, Optional<TestRunItems>> runItemProvider) {
        final boolean matchesSearch = query.isEmpty() || readsQuery(testCase, query);
        final boolean matchesPriority = priorities.isEmpty() || priorities.contains(testCase.getPriority());
        final boolean matchesGroup = groups.isEmpty()
                || (groups.contains(Group.UNASSIGNED) && testCase.getGroup().isEmpty())
                || testCase.getGroup().stream().anyMatch(groups::contains);
        final boolean matchesModule = modules.isEmpty() || modules.contains(testCase.getModule());
        final boolean matchesStatus = statuses.isEmpty()
                || matchesStatus(testCase.getId(), statuses, runItemProvider);

        return matchesSearch && matchesPriority && matchesGroup && matchesModule && matchesStatus;
    }

    /**
     * UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-091.
     * <p>
     * Whether any field the test case carries holds what was typed.
     * <p>
     * It read four - the description, the identity, the expected result and the
     * steps - and knew nothing of the module, the group, the test data, the
     * pre-conditions or the reference, each of which has its own column and
     * three of which have their own filter. A tester searching for a module
     * found nothing (#212).
     * <p>
     * The list is written out here and it should not be: the global search asks
     * the same question of all eighteen attributes, in Hits, so there are two
     * answers to "does this case hold this text". One owner needs a Project -
     * TestEditorAttributes reads a value through an extractor that takes one -
     * and this class has none, which is what keeps its two tests free of an IDE
     * (#294).
     */
    private static boolean readsQuery(final @NotNull TestCaseDto tc, final @NotNull String query) {
        return containsIgnoreCase(tc.getDescription(), query)
                || containsIgnoreCase(tc.getId().toString(), query)
                || containsIgnoreCase(tc.getExpectedResult(), query)
                || containsIgnoreCase(tc.getPreConditions(), query)
                || containsIgnoreCase(tc.getTestData(), query)
                || containsIgnoreCase(tc.getModule(), query)
                || containsIgnoreCase(tc.getReference(), query)
                || tc.getGroup().stream().anyMatch(group -> containsIgnoreCase(group.getName(), query))
                || tc.getSteps().stream().anyMatch(step -> containsIgnoreCase(step, query));
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
