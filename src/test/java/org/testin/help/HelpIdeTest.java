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


package org.testin.help;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;

import java.util.Arrays;
import java.util.List;

public class HelpIdeTest extends BasePlatformTestCase {

    // Rule-INTERNAL-127
    public void testAFiredHintWaitsUntilItsTopicIsCleared() {
        final @NotNull Hints hints = Services.getInstance(getProject(), Hints.class);

        hints.fire(Hint.of(Guide.RAISE_BUG_REPORTS, "Add bugRepoUrl to testin.yml"));
        hints.fire(Hint.of(Guide.RAISE_BUG_REPORTS, "Not signed in to github.com"));
        assertEquals("a second hint on one topic did not replace the first", List.of("Not signed in to github.com"), hints.waiting().stream().map(Hint::text).toList());

        hints.clear(Guide.RAISE_BUG_REPORTS);
        assertTrue("a cleared hint still waits", hints.waiting().isEmpty());
    }

    // Rule-INTERNAL-127
    public void testHintsOnTwoTopicsWaitTogether() {
        final @NotNull Hints hints = Services.getInstance(getProject(), Hints.class);

        hints.fire(Hint.of(Guide.RAISE_BUG_REPORTS, "Add bugRepoUrl to testin.yml"));
        hints.fire(Hint.of(Guide.SET_UP_THIS_MACHINE, "Set the Testin folder"));

        assertEquals("two topics did not wait together", 2, hints.waiting().size());
        hints.clear(Guide.RAISE_BUG_REPORTS);
        hints.clear(Guide.SET_UP_THIS_MACHINE);
    }

    // Rule-INTERNAL-129
    public void testOfferedGuidesListInGuideOrderUntilRemoved() {
        final @NotNull Guides guides = Services.getInstance(getProject(), Guides.class);
        Arrays.stream(Guide.values()).forEach(guides::remove);
        assertTrue("a guide was still offered after every one was removed", guides.offered().isEmpty());

        guides.add(Guide.RAISE_BUG_REPORTS);
        guides.add(Guide.SET_UP_THIS_MACHINE);
        assertEquals("the offered guides are not in guide order", List.of(Guide.SET_UP_THIS_MACHINE, Guide.RAISE_BUG_REPORTS), guides.offered());

        guides.remove(Guide.RAISE_BUG_REPORTS);
        guides.remove(Guide.SET_UP_THIS_MACHINE);
        assertTrue("a removed guide is still offered", guides.offered().isEmpty());
    }
}
