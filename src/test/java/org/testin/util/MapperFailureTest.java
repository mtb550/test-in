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

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;


import static org.testng.Assert.*;

/**
 * What the serializer does when it cannot serialize.
 * <p>
 * It used to answer with an empty array, and {@code TestDataFiles} wrote that over
 * the file - so a failure was logged and then committed to disk as a zero-byte
 * marker or test case, taking the real content with it. Six markers in a real
 * data root were found that way, across two projects.
 * <p>
 * What made it invisible is that both callers in {@code ProjectIndexer} already
 * catch a failure here and refuse to write. They were correct all along; they
 * simply never received one, because the failure was converted into a valid
 * looking answer before it reached them.
 */
public class MapperFailureTest {

    @Test
    public void aSerializationFailureRaisesRatherThanReturningNothing() {
        final IllegalStateException failure = expectThrows(IllegalStateException.class,
                () -> RealMapper.build().writeValueAsBytes(new NotSerializable()));

        assertNotNull(failure.getMessage());
        assertTrue(failure.getMessage().contains("NotSerializable"),
                "the message names what could not be written: " + failure.getMessage());
    }

    @Test
    public void theSameIsTrueForTheStringForm() {
        final IllegalStateException failure = expectThrows(IllegalStateException.class,
                () -> RealMapper.build().writeValueAsString(new NotSerializable()));

        assertTrue(failure.getMessage().contains("NotSerializable"));
    }

    /**
     * The point of the two above: nothing that reaches a file can be empty. A
     * marker is at minimum a pair of braces, and a test case a good deal more.
     */
    @Test
    public void whatIsWrittenIsNeverEmpty() {
        final byte[] testCase = RealMapper.build().writeValueAsBytes(TestCaseDto.builder().description("a case").build());

        assertTrue(testCase.length > 0);
        assertTrue(new String(testCase).contains("a case"));
    }

    @Test
    public void aTestCaseSurvivesTheRoundTrip() {
        final Mapper mapper = RealMapper.build();
        final TestCaseDto original = TestCaseDto.builder().description("written and read back").build();

        final TestCaseDto readBack = mapper.readValue(mapper.writeValueAsString(original), TestCaseDto.class);

        assertEquals(readBack.getDescription(), original.getDescription());
        assertEquals(readBack.getId(), original.getId());
    }

    /**
     * Jackson cannot serialize a type with no properties and no annotations, so
     * this is a genuine failure rather than a mocked one.
     */
    private static final class NotSerializable {
    }
}
