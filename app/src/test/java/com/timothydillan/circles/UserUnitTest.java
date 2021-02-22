package com.timothydillan.circles;

import com.timothydillan.circles.Models.User;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserUnitTest {
    @Test
    public void userInitializationIsCorrect() {
        User user = new User("1", "Timothy", "Dillan", "timothy@timothy.com", "+6593528755", 290901, "TOKEN");
        assertEquals(user.getUid(), "1");
        assertEquals(user.getFirstName(), "Timothy");
        assertEquals(user.getLastName(), "Dillan");
        assertEquals(user.getEmail(), "timothy@timothy.com");
        assertEquals(user.getPhone(), "+6593528755");
        assertEquals(user.getCurrentCircleSession(), 290901);
        assertEquals(user.getToken(), "TOKEN");
        assertNull(user.getProfilePicUrl());
    }
}
