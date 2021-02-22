package com.timothydillan.circles;

import com.timothydillan.circles.Models.Circle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CircleUnitTest {
    @Test
    public void circleInitializationIsCorrect() {
        Circle circle = new Circle("Admin");
        assertEquals(circle.getMemberRole(), "Admin");
    }
}
