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

import org.testin.testcase.TestEditorAttributes;
import org.testin.testrun.RunEditorAttributes;
import org.testng.annotations.Test;

import static org.testng.Assert.assertSame;

public class AttributeOrderTest {

    @Test
    public void orderIsTheFirstTestAttribute() {
        assertSame(TestEditorAttributes.values()[0], TestEditorAttributes.ORDER);
    }

    @Test
    public void orderIsTheFirstRunAttribute() {
        assertSame(RunEditorAttributes.values()[0], RunEditorAttributes.ORDER);
    }
}
