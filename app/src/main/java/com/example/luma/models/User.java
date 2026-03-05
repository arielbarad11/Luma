package com.example.luma.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    public String id;
    public String firstName;
    public String email;
    public String password;
    public int age;
    // שינוי ל-Map כדי למנוע את השגיאה ב-Firebase
    public Map<String, Goal> goals;
    public Map<String, String> crisisTime;
    public boolean admin;
    public String profileImage;

    public User() {
        this.goals = new HashMap<>();
        this.crisisTime = new HashMap<>();
    }

    public User(String id, String firstName, String email, String password, int age, 
                Map<String, Goal> goals, Map<String, String> crisisTime, boolean admin) {
        this.id = id;
        this.firstName = firstName;
        this.email = email;
        this.password = password;
        this.age = age;
        this.goals = goals != null ? goals : new HashMap<>();
        this.crisisTime = crisisTime != null ? crisisTime : new HashMap<>();
        this.admin = admin;
    }

    // מתודות עזר להמרת המפות לרשימות (בשביל ה-Adapters ב-UI)
    public List<Goal> getGoalsList() {
        if (goals == null) return new ArrayList<>();
        return new ArrayList<>(goals.values());
    }

    public List<String> getCrisisTimeList() {
        if (crisisTime == null) return new ArrayList<>();
        return new ArrayList<>(crisisTime.values());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    
    public Map<String, Goal> getGoals() { return goals; }
    public void setGoals(Map<String, Goal> goals) { this.goals = goals; }
    public Map<String, String> getCrisisTime() { return crisisTime; }
    public void setCrisisTime(Map<String, String> crisisTime) { this.crisisTime = crisisTime; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}