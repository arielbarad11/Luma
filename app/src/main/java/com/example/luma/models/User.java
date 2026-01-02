package com.example.luma.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList;

@IgnoreExtraProperties
public class User {

    public String id;
    public String firstName;
    public String email;
    public String password;
    public int age;
    public ArrayList<Goal> goals;
    public ArrayList<String> crisisTime;
    public boolean admin;

    public User() {}

    public User(String id, String firstName, String email, String password,
                int age, ArrayList<Goal> goals, ArrayList<String> crisisTime, boolean admin) {
        this.id = id;
        this.firstName = firstName;
        this.email = email;
        this.password = password;
        this.age = age;
        this.goals = goals;
        this.crisisTime = crisisTime;
        this.admin = admin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public ArrayList<Goal> getGoals() {
        return goals;
    }

    public void setGoals(ArrayList<Goal> goals) {
        this.goals = goals;
    }

    public ArrayList<String> getCrisisTime() {
        return crisisTime;
    }

    public void setCrisisTime(ArrayList<String> crisisTime) {
        this.crisisTime = crisisTime;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        User user = (User) object;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
