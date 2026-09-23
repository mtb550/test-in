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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.TestProjectMarker;

import java.util.Optional;

public class TestProjectFormatIdeTest extends BasePlatformTestCase {

    private static @NotNull Optional<String> refusalFor(final int format) {
        final @NotNull TestProjectMarker marker = new TestProjectMarker();
        marker.setFormat(format);

        return marker.whyNotReadable();
    }

    // Rule-INTERNAL-091
    public void testTheFormatThisBuildWritesIsRead() {
        assertTrue("a project in this build's own format has nothing to refuse",
                refusalFor(TestProjectMarker.FORMAT).isEmpty());
    }

    // Rule-INTERNAL-091
    public void testAProjectWithNoFormatIsRefusedAndNamesTheReleaseThatConvertsIt() {
        final @NotNull Optional<String> refused = refusalFor(0);

        assertTrue("a project written before the format number is not read at all", refused.isPresent());
        assertTrue("the refusal has to name the release that brings it forward, or the tester cannot act on it: "
                        + refused.orElseThrow(),
                refused.orElseThrow().contains(TestProjectMarker.CONVERTING_RELEASE));
    }

    // Rule-INTERNAL-091
    public void testAProjectFromANewerTestinIsRefused() {
        assertTrue("a format this build does not know is refused rather than guessed at",
                refusalFor(TestProjectMarker.FORMAT + 1).isPresent());
    }
}
