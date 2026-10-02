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

package org.testin.util;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class SeparatedValuesTest {

    @Test
    public void theSeparatorIsTheOneAskedFor() {
        assertEquals(SeparatedValues.split("a\tb,c\r\nd\te", '\t'), List.of(List.of("a", "b,c"), List.of("d", "e")));
        assertEquals(SeparatedValues.split("a\tb,c\nd,e", ','), List.of(List.of("a\tb", "c"), List.of("d", "e")));
    }

    // Rule-SHARE-124
    @Test
    public void aQuoteOpensAQuotedValueOnlyAtItsStart() {
        assertEquals(SeparatedValues.split("\"a,\"\"b\"\"\nc\",ab\"c\"d", ','), List.of(List.of("a,\"b\"\nc", "ab\"c\"d")));
    }

    // Rule-SHARE-124
    @Test
    public void aByteOrderMarkAtTheStartIsIgnored() {
        assertEquals(SeparatedValues.split("﻿a\tb", '\t'), List.of(List.of("a", "b")));
    }

    @Test
    public void anEmptyLineIsARowOfOneEmptyValueAndAFinalLineBreakIsNot() {
        assertEquals(SeparatedValues.split("a\r\n\r\nb\r\n", '\t'), List.of(List.of("a"), List.of(""), List.of("b")));
    }
}
