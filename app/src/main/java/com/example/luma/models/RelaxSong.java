package com.example.luma.models;

public class RelaxSong {
    private String title;
    private int audioResourceId; // הקובץ ב-raw
    private int imageResourceId; // התמונה ב-drawable

    public RelaxSong(String title, int audioResourceId, int imageResourceId) {
        this.title = title;
        this.audioResourceId = audioResourceId;
        this.imageResourceId = imageResourceId;
    }

    // Getters
    public String getTitle() { return title; }
    public int getAudioResourceId() { return audioResourceId; }
    public int getImageResourceId() { return imageResourceId; }
}