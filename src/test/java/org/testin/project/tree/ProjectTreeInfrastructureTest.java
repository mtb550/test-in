package org.testin.project.tree;

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
