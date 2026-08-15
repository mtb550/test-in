package org.testin.git;

import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.enums.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCaseChangeComparatorTest {

    @Test
    public void comparesAllEditableFields() {
        final TestCaseDto oldState = TestCaseDto.builder().build();
        final TestCaseDto newState = TestCaseDto.builder()
                .description("new description")
                .expectedResult("new result")
                .steps(List.of("first", "second"))
                .priority(Priority.HIGH)
                .status(TestCaseStatus.REVIEWED)
                .reference("REF-1")
                .module("payments")
                .testData("account=1")
                .preConditions("authenticated")
                .group(List.of(Group.SMOKE))
                .build();

        final List<FieldChange> changes = TestCaseChangeComparator.compare(oldState, newState);

        assertEquals(changes.size(), 10);
        assertTrue(changes.stream().anyMatch(change -> change.changeType() == ChangeType.CHANGE_STEPS));
        assertTrue(changes.stream().anyMatch(change -> change.changeType() == ChangeType.CHANGE_PRECONDITIONS));
    }
}
