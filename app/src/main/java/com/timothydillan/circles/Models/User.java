package com.timothydillan.circles.Models;

public class User {
    public String uid;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public int currentCircleSession;
    public double latitude;
    public double longitude;

    public User() {

    }

    public User(String uid, String firstName, String lastName, String email, String phone, int currentCircleSession) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.currentCircleSession = currentCircleSession;
    }

    public String getFullName() {return firstName + " " + lastName;}

    public String getFirstName() {
        return firstName;
    }

    public int getCurrentCircleSession() { return currentCircleSession; }

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getUid() { return uid; }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
