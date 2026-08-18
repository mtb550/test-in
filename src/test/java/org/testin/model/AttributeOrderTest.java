package org.testin.model;

import org.testng.annotations.Test;

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
