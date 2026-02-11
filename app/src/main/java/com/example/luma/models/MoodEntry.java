package com.example.luma.models;

import java.util.Date;

public class MoodEntry {
    public String id, userId, notes;
    public Date timestamp;
    public int moodValue;

    public MoodEntry() {
    } // חובה עבור Firebase

    public MoodEntry(String id, String userId, Date timestamp, int moodValue, String notes) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.moodValue = moodValue;
        this.notes = notes;
    }
}