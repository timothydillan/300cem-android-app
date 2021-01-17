package com.timothydillan.circles.Models;

public class User {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private int currentCircleSession;
    public User(String firstName, String lastName, String email, String phone, int currentCircleSession) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.currentCircleSession = currentCircleSession;
    }
}
