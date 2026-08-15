package org.testin.editor;

import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.enums.TestStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

        final List<TestCaseDto> result = TestCaseFilter.filter(
                List.of(passed, missing),
                "",
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(TestStatus.PASSED),
                Map.of(passed.getId(), item)::get);

        assertEquals(result, List.of(passed));
    }
}
