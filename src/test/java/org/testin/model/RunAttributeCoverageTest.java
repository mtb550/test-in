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

package org.testin.model;

import org.testin.testrun.RunEditorAttributes;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RunAttributeCoverageTest {

    @Test
    public void everyFailureDetailIsAlsoARunAttribute() {
        assertEquals(FailureDetail.values().length, 6,
                "a seventh thing a failure records needs a RunEditorAttributes constant too, "
                        + "or it is stored, cleared on a pass, and shown nowhere");

        final List<String> named = Arrays.stream(RunEditorAttributes.values())
                .map(RunEditorAttributes::getName)
                .toList();

        for (final String expected : List.of("Actual Result", "Stacktrace", "Bug Severity", "Bug Priority", "Bug Issue")) {
            assertTrue(named.contains(expected), expected + " is recorded on a run row but has no attribute: " + named);
        }
    }

    @Test
    public void theStacktraceIsReadOnlyAndOffByDefault() {
        assertFalse(RunEditorAttributes.STACKTRACE.isEdited(),
                "the framework writes it; the tester edits it through the failure dialog, not a grid cell");
        assertEquals(RunEditorAttributes.STACKTRACE.getToolBarDefault(), ToolBarDefault.OFF,
                "it is measured in paragraphs - a column of it on by default crowds out every other column");
    }

    @Test
    public void theDetailsPanelDrawsEveryRunValueWorthReading() {
        final List<RunEditorAttributes> shown = List.of(
                RunEditorAttributes.RUN_STATUS,
                RunEditorAttributes.DURATION,
                RunEditorAttributes.ACTUAL_RESULT,
                RunEditorAttributes.STACKTRACE,
                RunEditorAttributes.BUG_SEVERITY,
                RunEditorAttributes.BUG_PRIORITY,
                RunEditorAttributes.BUG_ISSUE);

        assertEquals(shown.size(), 7, "the rows DetailsTab appends for a case viewed under a run");
        assertEquals(shown.stream().distinct().count(), 7L, "each drawn once");
    }
}
