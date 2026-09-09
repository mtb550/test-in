package org.testin.editor;

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
                "  LOGIN ",
                Set.of("Regression"),
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

    /**
     * The defect #212 reported, and the reason #294 existed: the module has its
     * own column and its own filter, and the editor's search box could not see
     * it while the global search could. Both ask
     * {@link org.testin.model.TestEditorAttributes#anyContains} now.
     * <p>
     * That this test needs no IDE is the point. Routing the question through the
     * attributes used to mean handing them a Project, and there is no way to
     * build one here - which is what kept the two answers apart.
     */
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
            final List<TestCaseDto> result = TestCaseFilter.filter(List.of(tc), wanted, Set.of(), Set.of(), Set.of());

            assertEquals(result, List.of(tc), "the search did not read the field holding '" + wanted + "'");
        }
    }

    @Test
    public void aQueryNoFieldHoldsFindsNothing() {
        final TestCaseDto tc = TestCaseDto.builder().description("Log in").build();

        assertEquals(TestCaseFilter.filter(List.of(tc), "nothing holds this", Set.of(), Set.of(), Set.of()), List.of());
    }
}
