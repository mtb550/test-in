package org.testin.editor;

import org.testin.model.Group;
import org.testin.model.Priority;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestCaseFilterTest {

    @Test
    public void filtersSearchAndMetadataTogether() {
        final TestCaseDto matching = TestCaseDto.builder()
                .description("Login succeeds")
                .expectedResult("Dashboard")
                .priority(Priority.HIGH)
                .group(List.of(Group.REGRESSION))
                .module("accounts")
                .build();
        final TestCaseDto other = TestCaseDto.builder()
                .description("Logout")
                .priority(Priority.LOW)
                .module("accounts")
                .build();

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(matching, other),
                "  LOGIN ",
                Set.of(Group.REGRESSION),
                Set.of(Priority.HIGH),
                Set.of("accounts"));

        assertEquals(result, List.of(matching));
    }

    @Test
    public void filtersRunStatusOnlyWhenRunItemExists() {
        final TestCaseDto passed = TestCaseDto.builder().description("passed").build();
        final TestCaseDto missing = TestCaseDto.builder().description("missing").build();
        final TestRunItems item = TestRunItems.builder()
                .id(passed.getId())
                .status(TestStatus.PASSED)
                .tc(passed)
                .build();

        // The filter asks for an answer per id, and a case the run never
        // recorded answers with nothing rather than with a null.
        final Map<UUID, TestRunItems> recorded = Map.of(passed.getId(), item);

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(passed, missing),
                "",
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(TestStatus.PASSED),
                id -> Optional.ofNullable(recorded.get(id)));

        assertEquals(result, List.of(passed));
    }
}
