package com.example.luma.screens;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.luma.R;
import com.example.luma.models.RelaxSong;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private ImageView albumArt;
    private TextView songTitle;
    private FloatingActionButton btnPlayPause;
    private List<RelaxSong> playlist;
    private int currentIndex = 0;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        playlist = new ArrayList<>();
        // שחררי מהערה רק אם הקבצים קיימים, אחרת זה יקרוס
        // playlist.add(new RelaxSong("גלי ים מרגיעים", R.raw.sea_waves, R.drawable.ocean_pic));
        //playlist.add(new RelaxSong("ציפורי יער", R.raw.birds_forest, R.drawable.forest_pic));

        initUI();

        if (!playlist.isEmpty()) {
            loadSong(currentIndex);
        }
    }

    private void initUI() {
        albumArt = findViewById(R.id.album_art);
        songTitle = findViewById(R.id.tv_item_user_name); // וודאי שה-ID הזה קיים ב-XML
        btnPlayPause = findViewById(R.id.btn_play_pause);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
    }

    private void loadSong(int index) {
        RelaxSong current = playlist.get(index);
        albumArt.setImageResource(current.getImageResourceId());
        if (songTitle != null) songTitle.setText(current.getTitle());

        if (mediaPlayer != null) mediaPlayer.release();

        mediaPlayer = MediaPlayer.create(this, current.getAudioResourceId());
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (isPlaying) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        }
        isPlaying = !isPlaying;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }
}