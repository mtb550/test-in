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

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class AgentTranscriptTest {

    // Rule-CODEGEN-089
    @Test
    public void theCountIsTheBodiesThatLanded() {
        final AgentTranscript transcript = new AgentTranscript();

        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Log in").build(), "prompt", "statements", true, "written");
        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Log out").build(), "prompt", "I am sorry", false, "not Java");
        transcript.record(TestCaseDto.builder().id(UUID.randomUUID()).description("Lock").build(), "prompt", "statements", true, "written");

        assertEquals(transcript.landed(), 2);
    }
}
