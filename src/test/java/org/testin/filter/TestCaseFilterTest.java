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

package org.testin.filter;

import org.testin.model.Priority;
import org.testin.model.status.TestCaseStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.RunItemStatus;
import org.testin.model.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.testcase.TestCaseEditorAttributes;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class TestCaseFilterTest {

    @Test
    public void filtersSearchAndMetadataTogether() {
        final TestCaseDto matching = TestCaseDto.builder()
                .description("Login succeeds")
                .expectedResult("Dashboard")
                .priority(Priority.HIGH)
                .group(List.of("Regression"))
                .module("accounts")
                .build();
        final TestCaseDto other = TestCaseDto.builder()
                .description("Logout")
                .priority(Priority.LOW)
                .module("accounts")
                .build();

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(matching, other),
                new FilterSelection("  LOGIN ", Set.of("Regression"), Set.of(Priority.HIGH), Set.of("accounts"), Set.of(), Set.of(), Set.of()));

        assertEquals(result, List.of(matching));
    }

    @Test
    public void filtersRunItemStatusOnlyWhenRunItemExists() {
        final TestCaseDto passed = TestCaseDto.builder().description("passed").build();
        final TestCaseDto missing = TestCaseDto.builder().description("missing").build();
        final TestRunItems item = TestRunItems.builder()
                .id(passed.getId())
                .status(RunItemStatus.PASSED)
                .build()
                .showing(Optional.of(passed), Optional.empty(), Optional.empty());

        final Map<UUID, TestRunItems> recorded = Map.of(passed.getId(), item);

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(passed, missing),
                new FilterSelection("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(RunItemStatus.PASSED), Set.of()),
                id -> Optional.ofNullable(recorded.get(id)));

        assertEquals(result, List.of(passed));
    }

    @Test
    public void theSearchReadsEveryFieldTheTesterWrites() {
        final TestCaseDto tc = TestCaseDto.builder()
                .description("Log in")
                .module("accounts")
                .testData("admin@example.com")
                .preConditions("The account exists")
                .reference("JIRA-123")
                .group(List.of("Regression"))
                .build();

        for (final String wanted : List.of("accounts", "admin@example.com", "The account exists", "JIRA-123", "Regression")) {
            final List<TestCaseDto> result = TestCaseFilter.filter(List.of(tc), new FilterSelection(wanted, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));

            assertEquals(result, List.of(tc), "the search did not read the field holding '" + wanted + "'");
        }
    }

    @Test
    public void filtersOnTheTestCaseStatus() {
        final TestCaseDto reviewed = TestCaseDto.builder().description("reviewed").status(TestCaseStatus.REVIEWED).build();
        final TestCaseDto pending = TestCaseDto.builder().description("pending").status(TestCaseStatus.PENDING).build();

        final List<TestCaseDto> result = TestCaseFilter.filter(List.of(reviewed, pending), new FilterSelection("", Set.of(), Set.of(), Set.of(), Set.of(TestCaseStatus.REVIEWED), Set.of(), Set.of()));

        assertEquals(result, List.of(reviewed));
    }

    @Test
    public void aQueryNoFieldHoldsFindsNothing() {
        final TestCaseDto tc = TestCaseDto.builder().description("Log in").build();

        assertEquals(TestCaseFilter.filter(List.of(tc), new FilterSelection("nothing holds this", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())), List.of());
    }

    // Rule-EDITOR-PANEL-091
    @Test
    public void theSearchReadsEveryFieldTheFieldsPopupLists() {
        final @NotNull TestSetDirectoryDto set = new TestSetDirectoryDto();
        set.setPath2(new ArrayList<>(List.of("Shop", "Checkout")));
        final @NotNull TestCaseDto tc = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Refuse an expired card")
                .expectedResult("The payment is declined")
                .steps(List.of("Enter the card number"))
                .priority(Priority.HIGH)
                .reference("JIRA-77")
                .testData("card=4111")
                .preConditions("A basket holds one item")
                .group(List.of("Regression"))
                .module("payments")
                .status(TestCaseStatus.REVIEWED)
                .createdBy("Sara")
                .updatedBy("Omar")
                .createdAt(ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC))
                .updatedAt(ZonedDateTime.of(2026, 6, 7, 8, 9, 10, 0, ZoneOffset.UTC))
                .build();
        tc.setParent(set);

        final @NotNull List<String> notFound = new ArrayList<>();
        for (final TestCaseEditorAttributes field : TestCaseEditorAttributes.values()) {
            final @NotNull String value = field.gridValue(tc);
            if (value.isBlank()) continue;

            if (TestCaseFilter.filter(List.of(tc), new FilterSelection(value, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())).isEmpty()) notFound.add(field.getName() + " = " + value);
        }

        assertEquals(notFound, List.of(), "the search does not read these fields");
    }

    // Rule-EDITOR-PANEL-096
    @Test
    public void choosingNothingInAFilterMatchesEveryTestCase() {
        final @NotNull TestCaseDto high = TestCaseDto.builder().description("one").priority(Priority.HIGH).group(List.of("Smoke")).module("accounts").status(TestCaseStatus.REVIEWED).build();
        final @NotNull TestCaseDto low = TestCaseDto.builder().description("two").priority(Priority.LOW).module("payments").status(TestCaseStatus.DISABLED).build();
        final @NotNull TestCaseDto bare = TestCaseDto.builder().description("three").build();

        final @NotNull List<TestCaseDto> result = TestCaseFilter.filter(List.of(high, low, bare), new FilterSelection("", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), _ -> Optional.empty());

        assertEquals(result, List.of(high, low, bare));
    }
}
