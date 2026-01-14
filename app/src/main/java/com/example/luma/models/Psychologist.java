package com.example.luma.models;

import androidx.annotation.NonNull;

public class Psychologist {
    String id;
    String email;

    String name;

    String city;

    int sessionPrice;


    public Psychologist() {
    }

    public Psychologist(String id, String email, String name, int sessionPrice) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.city = city;
        this.sessionPrice = sessionPrice;
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

    public void setName(String name){
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city){
        this.city = city;
    }

    public int getSessionPrice() {
        return sessionPrice;
    }

    public void setName(int sessionPrice){
        this.sessionPrice = sessionPrice;
    }

    @NonNull
    @Override
    public String toString() {
        return "Psychologist{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", sessionPrice='" + sessionPrice + '\'' +
                '}';
    }
}
