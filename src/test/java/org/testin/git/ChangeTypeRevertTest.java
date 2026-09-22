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

package org.testin.git;

import org.testin.model.Priority;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ChangeTypeRevertTest {

    private static TestCaseDto committed() {
        return TestCaseDto.builder()
                .description("committed description")
                .expectedResult("committed result")
                .steps(new ArrayList<>(List.of("committed first", "committed second")))
                .priority(Priority.LOW)
                .status(TestCaseStatus.PENDING)
                .reference("REF-committed")
                .module("committed module")
                .testData("committed data")
                .preConditions("committed preconditions")
                .group(new ArrayList<>(List.of("Smoke")))
                .build();
    }

    private static TestCaseDto edited() {
        return TestCaseDto.builder()
                .description("edited description")
                .expectedResult("edited result")
                .steps(new ArrayList<>(List.of("edited first")))
                .priority(Priority.HIGH)
                .status(TestCaseStatus.REVIEWED)
                .reference("REF-edited")
                .module("edited module")
                .testData("edited data")
                .preConditions("edited preconditions")
                .group(new ArrayList<>(List.of("Regression")))
                .build();
    }

    @Test
    public void everyChangeTheReviewCanShowCanBeReverted() {
        final List<FieldChange> changes = TestCaseChangeComparator.compare(committed(), edited());

        assertEquals(changes.size(), 10, "every editable field differs between the two states");
        for (final FieldChange change : changes) {
            assertTrue(change.changeType().isRevertible(),
                    change.changeType() + " appears in a review, so it must be revertible");
        }
    }

    @Test
    public void creatingOrRemovingAWholeTestCaseHasNoFieldToRevert() {
        assertFalse(ChangeType.CREATE_TEST_CASE.isRevertible());
        assertFalse(ChangeType.REMOVE_TEST_CASE.isRevertible());
    }

    @Test
    public void eachRevertRestoresItsOwnFieldAndNothingElse() {
        for (final FieldChange change : TestCaseChangeComparator.compare(committed(), edited())) {
            final TestCaseDto current = edited();
            change.changeType().getRevertAction().apply(current, committed());

            final List<FieldChange> left = TestCaseChangeComparator.compare(committed(), current);

            assertEquals(left.size(), 9,
                    change.changeType() + " should revert exactly one field, leaving nine changed");
            assertTrue(left.stream().noneMatch(remaining -> remaining.changeType() == change.changeType()),
                    change.changeType() + " reverted something other than its own field");
        }
    }

    @Test
    public void revertingEveryChangeRestoresTheCommittedTestCase() {
        final TestCaseDto current = edited();
        for (final FieldChange change : TestCaseChangeComparator.compare(committed(), edited())) {
            change.changeType().getRevertAction().apply(current, committed());
        }

        assertEquals(TestCaseChangeComparator.compare(committed(), current), List.of(),
                "reverting every listed change leaves nothing to review");
    }

    @Test
    public void revertingTheLastChangePutsBackTheCommittedAudit() {
        final TestCaseDto committed = committed()
                .setCreatedBy("Sara Al-Otaibi")
                .setUpdatedBy("Sara Al-Otaibi")
                .setUpdatedAt(ZonedDateTime.parse("2026-09-01T10:00:00Z"));
        final TestCaseDto current = committed().setModule("edited module").setUpdatedBy("Muteb");

        ChangeType.CHANGE_MODULE.getRevertAction().apply(current, committed);
        assertEquals(TestCaseChangeComparator.compare(committed, current), List.of(), "nothing reviewable is left");

        current.takeAuditOf(committed);

        assertEquals(current.getCreatedBy(), committed.getCreatedBy());
        assertEquals(current.getCreatedAt(), committed.getCreatedAt());
        assertEquals(current.getUpdatedBy(), committed.getUpdatedBy(), "the reverting tester's stamp stayed on a case nobody changed");
        assertEquals(current.getUpdatedAt(), committed.getUpdatedAt());
    }

    @Test
    public void revertingAListCopiesItRatherThanSharingIt() {
        final TestCaseDto committed = committed();
        final TestCaseDto current = edited();

        ChangeType.CHANGE_STEPS.getRevertAction().apply(current, committed);
        ChangeType.CHANGE_GROUP.getRevertAction().apply(current, committed);

        current.getSteps().add("typed after the revert");
        current.getGroup().add("Regression");

        assertEquals(committed.getSteps(), List.of("committed first", "committed second"),
                "the committed steps were shared, not copied");
        assertEquals(committed.getGroup(), List.of("Smoke"),
                "the committed groups were shared, not copied");
    }

    @Test
    public void noTwoChangeTypesShareALabel() {
        final Set<String> labels = new HashSet<>();
        for (final ChangeType type : ChangeType.values()) {
            assertTrue(labels.add(type.getLabel()), type + " repeats another change type's label");
        }
    }

}
