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

import org.testng.annotations.Test;
import org.testin.testrun.RunEditorAttributes;
import org.testin.testcase.TestEditorAttributes;

import static org.testng.Assert.assertSame;

/**
 * ORDER is the first constant of both attribute enums.
 * <p>
 * A grid column carries its attribute's ordinal as its model index, and three
 * things then ask for model column 0 by name: the one cell that is never
 * editable, the click that selects a whole row, and ENTER opening the details
 * view. A constant declared above ORDER would move all three onto the attribute
 * beside it, and nothing would fail - the grid would simply act on the wrong
 * column, quietly. That is what this pins.
 */
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
