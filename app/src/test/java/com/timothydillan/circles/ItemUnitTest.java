package com.timothydillan.circles;

import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.Item;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ItemUnitTest {
    private Item testItem = new Item();

    @Test
    public void itemInitializationIsCorrect() {
        testItem.addItem("Test", "TestValue");
        testItem.addItem("Test1", "TestValue1");
        assertEquals(testItem.getItemValue(0), "TestValue");
        assertEquals(testItem.getItemName(0), "Test");
        assertEquals(testItem.getItemValue(1), "TestValue1");
        assertEquals(testItem.getItemName(1), "Test1");
    }
}