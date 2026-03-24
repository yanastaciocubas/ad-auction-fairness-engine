package com.yonara.adauction.model;

public class UserProfile {

    private String userId;
    private String ageGroup;
    private String gender;
    private String incomeLevel;

    public UserProfile() {}

    public UserProfile(String userId, String ageGroup,
                       String gender, String incomeLevel) {
        this.userId = userId;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.incomeLevel = incomeLevel;
    }

    public String getUserId() { return userId; }
    public String getAgeGroup() { return ageGroup; }
    public String getGender() { return gender; }
    public String getIncomeLevel() { return incomeLevel; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    public void setGender(String gender) { this.gender = gender; }
    public void setIncomeLevel(String incomeLevel) { this.incomeLevel = incomeLevel; }
}