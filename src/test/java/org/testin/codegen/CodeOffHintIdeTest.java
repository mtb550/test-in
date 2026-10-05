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


package org.testin.codegen;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.help.Hint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.services.Services;

import java.util.List;

public class CodeOffHintIdeTest extends BasePlatformTestCase {

    private @NotNull List<Hint> waiting() {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == SetupStep.TEST_PROJECT_LINK).toList();
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public void testCodeOffIsAHintWithSaveToTestinYmlAndNoMessage() {
        final @NotNull List<String> said = Said.during(getProject(), () -> assertFalse("code is on with no testin.yml", CodeOn.isOnOrHinted(getProject())));

        assertEquals("a message was raised while code updated", List.of(), said);
        assertEquals("code off did not wait as one hint", 1, waiting().size());
        assertTrue("the hint carries no Save to testin.yml", waiting().getFirst().form().isPresent());
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public void testAPressedKeyStillAnswersAndTheHintWaitsBesideIt() {
        final @NotNull List<String> said = Said.during(getProject(), () -> assertTrue(CodeOn.isOffAndWarned(getProject())));

        assertEquals("the key was not answered", 1, said.size());
        assertEquals("code off did not wait as one hint", 1, waiting().size());
        Services.getInstance(getProject(), Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
    }
}
