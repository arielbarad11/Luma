package com.example.luma.models;

import java.util.ArrayList;
import java.util.List;

public class Goal {
    private String id;
    private String title;
    private ArrayList<String> days;  // ← ArrayList במקום List — Firebase דורש זאת
    private int hour;
    private int minute;
    private boolean isActive;

    public Goal() {
        // קונסטרקטור ריק בשביל Firebase
    }

    public Goal(String id, String title, List<String> days, int hour, int minute) {
        this.id = id;
        this.title = title;
        this.days = new ArrayList<>(days);  // ← המרה ל-ArrayList
        this.hour = hour;
        this.minute = minute;
        this.isActive = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ArrayList<String> getDays() { return days; }
    public void setDays(ArrayList<String> days) { this.days = days; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getFormattedTime() {
        return String.format("%02d:%02d", hour, minute);
    }
}