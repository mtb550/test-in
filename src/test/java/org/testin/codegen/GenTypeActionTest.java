package org.testin.codegen;

import org.testin.codegen.method.update.NoOpCodeUpdate;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Which operations write code and which only write data, as the enum declares
 * it - and that both forms of the same operation, one item or many, go to the
 * same place.
 * <p>
 * They did not. {@code executeAll} went straight to the generator registry while
 * {@code getAction} went through the declared action, so a data-only attribute
 * edited in bulk asked for a Java generator it has no use for, and warned about
 * the missing Java plugin on the way in an IDE that was never going to generate
 * anything (#151).
 */
public class GenTypeActionTest {

    /**
     * The two constructors are the declaration: three arguments means the third
     * one names a data-only field and the operation carries the no-op. Pinned by
     * name rather than by count, so adding an operation does not fail this and
     * changing what an existing one does will.
     */
    @Test
    public void aDataOnlyAttributeCarriesTheNoOp() {
        for (final GenType type : List.of(
                GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
                GenType.UPDATE_TEST_CASE_MODULE,
                GenType.UPDATE_TEST_CASE_TEST_DATA,
                GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
                GenType.UPDATE_TEST_CASE_STEPS,
                GenType.UPDATE_TEST_CASE_PRIORITY,
                GenType.NO_CODE_CHANGE)) {

            assertTrue(type.getAction() instanceof NoOpCodeUpdate,
                    type + " writes no code, so it must carry the no-op rather than look for a generator");
        }
    }

    @Test
    public void anOperationThatWritesCodeDoesNot() {
        for (final GenType type : List.of(
                GenType.CREATE_TEST_CASE,
                GenType.REMOVE_TEST_CASE,
                GenType.UPDATE_TEST_CASE_DESCRIPTION,
                GenType.UPDATE_TEST_CASE_GROUP,
                GenType.UPDATE_TEST_CASE_ORDER,
                GenType.CREATE_TEST_SET,
                GenType.RENAME_TEST_SET)) {

            assertFalse(type.getAction() instanceof NoOpCodeUpdate,
                    type + " writes code, so it must reach a generator");
        }
    }

    /**
     * Every constant has one, which is the reason the field can hold a value
     * instead of a null standing for "look it up later".
     */
    @Test
    public void everyOperationCarriesAnAction() {
        assertEquals(Arrays.stream(GenType.values()).filter(type -> type.getAction() == null).count(), 0L);
    }
}
