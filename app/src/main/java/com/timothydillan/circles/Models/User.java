package com.timothydillan.circles.Models;

import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class User {
    public String firstName;
    public String lastName;
    public String email;
    public String phone = "";
    public int currentCircleSession;
    public double latitude;
    public double longitude;
    public String lastSharingTime;
    public String gender = "";
    public String birthDate = "";
    public String uid;
    public String profilePicUrl = "";
    public String token = "";

    public User() {

    }

    public User(String uid, String firstName, String lastName, String email, String phone, int currentCircleSession, String token) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.currentCircleSession = currentCircleSession;
        this.lastSharingTime = getCurrentDateAndTime();
        this.token = token;
    }

    public User(String uid, String firstName, String lastName, String email, int currentCircleSession, String token) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.currentCircleSession = currentCircleSession;
        this.lastSharingTime = getCurrentDateAndTime();
        this.token = token;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUid() {
        return uid;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLastSharingTime() { return lastSharingTime; }

    public void updateLastSharingTime() { this.lastSharingTime = getCurrentDateAndTime(); }

    public String getCurrentDateAndTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm EEEE");
        return dateFormat.format(new Date());
    }

    public void setCurrentCircleSession(int currentCircleSession) {
        this.currentCircleSession = currentCircleSession;
    }

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

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
}
