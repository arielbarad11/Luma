package com.example.luma.models;

import androidx.annotation.NonNull;

public class Psychologist {
    String id;
    String email;

    String name;


    public Psychologist() {
    }

    public Psychologist(String id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(){
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return "Psychologist{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
