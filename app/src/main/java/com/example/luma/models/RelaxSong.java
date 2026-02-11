package com.example.luma.models;

public class RelaxSong {
    private final String title;
    private final int audioResourceId; // הקובץ ב-raw
    private final int imageResourceId; // התמונה ב-drawable

    public RelaxSong(String title, int audioResourceId, int imageResourceId) {
        this.title = title;
        this.audioResourceId = audioResourceId;
        this.imageResourceId = imageResourceId;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public int getAudioResourceId() {
        return audioResourceId;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }
}