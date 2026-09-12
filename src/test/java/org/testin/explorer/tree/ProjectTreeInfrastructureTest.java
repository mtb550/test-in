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

package org.testin.explorer.tree;

import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testng.annotations.Test;

import javax.swing.*;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ProjectTreeInfrastructureTest {

    @Test
    public void transferFlavorUsesDirectPayloadClass() {
        assertEquals(TreeTransferHandler.NODE_FLAVOR.getRepresentationClass(), TreeTransferPayload.class);
    }

    @Test
    public void transferPayloadKeepsSelectedDirectories() {
        final DirectoryDto directory = new TestSetDirectoryDto();
        directory.setPath(Path.of("project", "test-cases", "set"));

        final TreeTransferPayload payload = new TreeTransferPayload(new DirectoryDto[]{directory});

        assertNotNull(payload.nodes());
        assertEquals(payload.nodes().length, 1);
        assertEquals(payload.nodes()[0], directory);
        assertEquals(payload.clipboardAction(), TransferHandler.COPY);
    }

    @Test
    public void transferPayloadPreservesCutOperation() {
        final TreeTransferPayload payload = new TreeTransferPayload(new DirectoryDto[0], TransferHandler.MOVE);

        assertEquals(payload.clipboardAction(), TransferHandler.MOVE);
    }

}
