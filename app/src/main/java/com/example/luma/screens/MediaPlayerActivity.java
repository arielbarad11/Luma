package com.example.luma.screens;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.luma.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MediaPlayerActivity extends AppCompatActivity {

    // שירים חינמיים מ-Internet Archive (public domain, ללא צורך ב-raw)
    private static final String[] URLS = {
            "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/bd/Gnossienne_No_1.ogg/Gnossienne_No_1.ogg.mp3",
            "https://upload.wikimedia.org/wikipedia/commons/transcoded/6/6a/Erik_Satie_-_Gymnopedie_No._1.ogg/Erik_Satie_-_Gymnopedie_No._1.ogg.mp3",
            "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/4e/BWV_543-fugue.ogg/BWV_543-fugue.ogg.mp3"
    };

    private static final String[] TITLES = {
            "Gnossienne No.1 – Erik Satie",
            "Gymnopédie No.1 – Erik Satie",
            "Fugue BWV 543 – Bach"
    };

    private MediaPlayer mediaPlayer;
    private FloatingActionButton btnPlayPause;
    private SeekBar seekBar;
    private TextView tvTitle, tvArtist;
    private ImageView albumArt;

    private boolean isPlaying = false;
    private int currentIndex = 0;

    private final Handler handler = new Handler();
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                seekBar.setMax(mediaPlayer.getDuration());
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        btnPlayPause = findViewById(R.id.btn_play_pause);
        seekBar      = findViewById(R.id.seekbar);
        tvTitle      = findViewById(R.id.tv_title);
        tvArtist     = findViewById(R.id.tv_artist);
        albumArt     = findViewById(R.id.album_art);

        loadSong(currentIndex);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void loadSong(int index) {
        tvTitle.setText(TITLES[index]);
        tvArtist.setText("מוזיקה מרגיעה 🎵");
        albumArt.setImageResource(android.R.drawable.ic_menu_gallery);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        isPlaying = false;
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);

        Toast.makeText(this, "טוען שיר...", Toast.LENGTH_SHORT).show();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        try {
            mediaPlayer.setDataSource(URLS[index]);
            mediaPlayer.prepareAsync(); // טעינה ברקע — לא חוסמת את ה-UI
            mediaPlayer.setOnPreparedListener(mp -> {
                Toast.makeText(this, "מוכן לנגינה ▶", Toast.LENGTH_SHORT).show();
                btnPlayPause.setEnabled(true);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                // מעבר אוטומטי לשיר הבא
                currentIndex = (currentIndex + 1) % URLS.length;
                loadSong(currentIndex);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "שגיאה בטעינת השיר", Toast.LENGTH_SHORT).show();
                return true;
            });
            btnPlayPause.setEnabled(false); // מושבת עד שהשיר מוכן
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (isPlaying) {
            mediaPlayer.pause();
            handler.removeCallbacks(updateSeekBar);
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            handler.post(updateSeekBar);
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        }
        isPlaying = !isPlaying;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}