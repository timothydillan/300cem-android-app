package com.timothydillan.circles;

public class User {
    public String firstName, lastName, email, phone, circleId;
    User(String firstName, String lastName, String email, String phone, String circleId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.circleId = circleId;
    }
}
