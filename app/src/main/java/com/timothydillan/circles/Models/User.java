package com.timothydillan.circles.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class User {

    public String firstName;
    public String lastName;
    public String email;
    public String phone = "";
    public String birthDate = "";
    public String profilePicUrl = "";
    public String token;
    public int currentCircleSession;
    public double latitude;
    public double longitude;
    public String lastSharingTime;
    public String uid;
    public String heartRate;
    public String type = "Normal";
    public String mood;
    public int myCircle;
    public HashMap<String, String> stepCount;
    public HashMap<String, String> cyclingActivity;
    public HashMap<String, String> runningActivity;
    public HashMap<String, String> walkActivity;

    public User() {

    }

    public User(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.lastSharingTime = getCurrentDayAndTime();
    }

    public User(String uid, String firstName, String lastName, String email, String phone, int currentCircleSession, String token) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.currentCircleSession = currentCircleSession;
        myCircle = currentCircleSession;
        this.lastSharingTime = getCurrentDayAndTime();
        this.token = token;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setMyCircle(int myCircle) {
        this.myCircle = myCircle;
    }

    public String getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public int getMyCircle() {
        return myCircle;
    }

    public HashMap<String, String> getCyclingActivity() {
        return cyclingActivity;
    }

    public void setCyclingActivity(HashMap<String, String> cyclingActivity) {
        this.cyclingActivity = cyclingActivity;
    }

    public HashMap<String, String> getRunningActivity() {
        return runningActivity;
    }

    public void setRunningActivity(HashMap<String, String> runningActivity) {
        this.runningActivity = runningActivity;
    }

    public HashMap<String, String> getWalkActivity() {
        return walkActivity;
    }

    public void setWalkActivity(HashMap<String, String> walkActivity) {
        this.walkActivity = walkActivity;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public HashMap<String, String> getStepCount() {
        return stepCount;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
    }

    public void setStepCount(HashMap<String, String> stepCount) {
        this.stepCount = stepCount;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUid() {
        return uid;
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

    public void updateLastSharingTime() { this.lastSharingTime = getCurrentDayAndTime(); }

    public String getCurrentDayAndTime() {
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

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
}
