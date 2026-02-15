package com.example.luma.models;

import java.util.List;

public class Goal {
    private String id;           // מזהה ייחודי (בשביל Firebase או התראות)
    private String title;        // שם המטרה (למשל: "אימון ריצה")
    private List<String> days;  // רשימת ימים
    private int hour;            // שעת ההתראה (0-23)
    private int minute;          // דקת ההתראה (0-59)
    private boolean isActive;    // האם ההתראה פעילה

    public Goal() {
        // קונסטרקטור ריק בשביל Firebase
    }
    
    public Goal(String id, String title, List<String> days, int hour, int minute) {
        this.id = id;
        this.title = title;
        this.days = days;
        this.hour = hour;
        this.minute = minute;
        this.isActive = true;
    }

    // Getters ו-Setters...

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getDays() {
        return days;
    }

    public void setDays(List<String> days) {
        this.days = days;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // מתודת עזר להצגת זמן מעוצב (למשל 09:05)
    public String getFormattedTime() {
        return String.format("%02d:%02d", hour, minute);
    }
}