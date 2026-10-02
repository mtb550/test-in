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

package org.testin.services;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.Await;

import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundWorkIdeTest extends BasePlatformTestCase {

    // Rule-CODEGEN-089
    public void testTheEndIsReportedWhenTheTesterCancels() {
        final AtomicBoolean reported = new AtomicBoolean();

        BackgroundWork.run(getProject(), "Writing bodies", "Writing failed", true, indicator -> indicator.cancel(), () -> reported.set(true));

        Await.until("a canceled work never reported how it ended", reported::get);
    }
}
