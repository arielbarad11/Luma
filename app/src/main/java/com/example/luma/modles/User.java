package com.example.luma.modles;

import java.util.ArrayList;

public class User {
    public String id;
    public String username;

    public String firstname;
    public String email;
    public String password;

    public String age;

    public ArrayList<Goal> goals; // מטרה אישית של כל משתמש

    public ArrayList<String> crisisTime; // תוכנית אישית בזמן משבר



    public boolean isAdmin;
}
