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

package org.testin.view.details;

import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DetailsFollowFieldsTest {

    // Rule-VIEW-PANEL-090
    @Test
    public void fromATestRunAFieldFollowsTheTestRunsListAndOneItDoesNotOfferStays() {
        final @NotNull Predicate<TestCaseEditorAttributes> shows = DetailsTab.shownInTheTestRun(Set.of(TestRunEditorAttributes.EXPECTED_RESULT));

        assertTrue(shows.test(TestCaseEditorAttributes.EXPECTED_RESULT), "a field ticked in the test run's Fields was hidden");
        assertFalse(shows.test(TestCaseEditorAttributes.STEPS), "a field unticked in the test run's Fields was shown");
        assertTrue(shows.test(TestCaseEditorAttributes.MODULE), "a field the test run's Fields does not offer was hidden, and nothing can show it again");
    }
}
