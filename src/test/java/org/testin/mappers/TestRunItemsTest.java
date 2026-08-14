package org.testin.mappers;

import org.testin.mappers.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.expectThrows;
import static org.testng.Assert.assertTrue;

/**
 * The two contracts {@code tc} serves (#48).
 * <p>
 * A run item read straight out of the run JSON has no test case attached, and
 * one whose case has since been deleted never gets one - {@code FailedResultDialog}
 * opens for both and must cope. The editor, on the other hand, drops the items it
 * cannot resolve and assigns a case to every item it keeps, so nothing that
 * reaches a renderer is missing one.
 * <p>
 * Same field, two contracts. {@code getTc()} is for the first, {@code requireTc()}
 * for the second.
 */
public class TestRunItemsTest {

    @Test
    public void aFreshItemHasNoTestCaseUntilTheEditorWiresOne() {
        assertNull(TestRunItems.builder().id(UUID.randomUUID()).build().getTc(),
                "tc is @JsonIgnore - deserialising a run never fills it");
    }

    @Test
    public void requireTcReturnsTheWiredCase() {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).build();
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).tc(tc).build();

        assertSame(item.requireTc(), tc);
    }

    @Test
    public void requireTcFailsByNameRatherThanAsANullPointer() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();

        final IllegalStateException thrown = expectThrows(IllegalStateException.class, item::requireTc);

        assertTrue(thrown.getMessage().contains(id.toString()),
                "the message names the item, so a broken invariant is diagnosable: " + thrown.getMessage());
    }

    @Test
    public void getTcStaysAvailableForTheCallersThatMustHandleNull() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();

        // FailedResultDialog renders "No longer in the test set" from exactly this.
        assertNull(item.getTc());
        assertEquals(item.getStatus(), org.testin.enums.TestStatus.PENDING, "an unrun item defaults to PENDING");
    }
}
