package com.example.luma.modles;

import java.util.ArrayList;

public class User {
    public String id;

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

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
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
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public User(String id, String firstname, String email, String password, String age, ArrayList<Goal> goals, ArrayList<String> crisisTime, boolean isAdmin) {
        this.id = id;
        this.firstName = firstname;
        this.email = email;
        this.password = password;
        this.age = age;
        this.goals = goals;
        this.crisisTime = crisisTime;
        this.isAdmin = isAdmin;


    }

    public User() {

    }

    public String firstName;
    public String email;
    public String password;

    public String age;

    public ArrayList<Goal> goals; // מטרה אישית של כל משתמש

    public ArrayList<String> crisisTime; // תוכנית אישית בזמן משבר



    public boolean isAdmin;
}
