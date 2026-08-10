package org.testin.testCase;

import org.testin.mappers.dto.TestCaseDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Result of {@link TestCaseSorter#sortTestCases}: the linked-list order plus the
 * ids that were not reachable from the head chain.
 */
public record SortResult(List<TestCaseDto> sortedList, Set<UUID> unsortedIds) {
}
