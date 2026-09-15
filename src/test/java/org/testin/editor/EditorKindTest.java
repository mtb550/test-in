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

package org.testin.editor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * The keys an editor kind builds are already stored in testers' IDE properties
 * (#312, N20). A changed word or shape would throw away the column widths and
 * Details choices every tester set, so the exact strings are pinned here.
 */
public class EditorKindTest {

    @Test
    public void theDetailsKeysAreTheOnesAlreadyStored() {
        assertEquals(EditorKind.TEST.detailsKey(4), "testin.selectedDetails.test.v4");
        assertEquals(EditorKind.RUN.detailsKey(7), "testin.selectedDetails.run.v7");
    }

    @Test
    public void theColumnWidthKeysAreTheOnesAlreadyStored() {
        assertEquals(EditorKind.TEST.columnWidthKey("Expected Result"), "testin.grid.colWidth.test.Expected Result");
        assertEquals(EditorKind.RUN.columnWidthKey("FQCN"), "testin.grid.colWidth.run.FQCN");
    }
}
