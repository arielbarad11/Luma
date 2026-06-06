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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.RelaxSong;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerActivity extends AppCompatActivity {

    private List<RelaxSong> songList;
    private int currentIndex = 0;

    private MediaPlayer mediaPlayer;
    private FloatingActionButton btnPlayPause, btnNext, btnPrev;
    private SeekBar seekBar;
    private TextView tvTitle, tvArtist;
    private ImageView albumArt;

    private boolean isPlaying = false;
    private boolean isPrepared = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    private final Handler handler = new Handler();
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying && isPrepared) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 500);
            }
        }
    };

    private final String[] URLS = {
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mediaPlayer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext      = findViewById(R.id.btn_next);
        btnPrev      = findViewById(R.id.btn_prev);
        seekBar      = findViewById(R.id.seekbar);
        tvTitle      = findViewById(R.id.tv_title);
        tvArtist     = findViewById(R.id.tv_artist);
        albumArt     = findViewById(R.id.album_art);

        btnPlayPause.setEnabled(false);
        btnNext.setEnabled(false);
        btnPrev.setEnabled(false);

        initSongList();
        loadSong(currentIndex);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        btnNext.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % songList.size();
            loadSong(currentIndex);
        });

        btnPrev.setOnClickListener(v -> {
            currentIndex = (currentIndex - 1 + songList.size()) % songList.size();
            loadSong(currentIndex);
        });

        // תיקון הגררה — seekTo עובד רק כשהנגן מוכן
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                // לא עושים כלום כאן
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                // משהמשתמש מתחיל לגרור — עוצרים את העדכון האוטומטי
                handler.removeCallbacks(updateSeekBar);
            }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                // רק כשמשחררים — מבצעים את הקפיצה
                if (mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo(sb.getProgress());
                }
                // מחדשים את העדכון האוטומטי אם מנגן
                if (isPlaying) {
                    handler.post(updateSeekBar);
                }
            }
        });
    }

    private void initSongList() {
        songList = new ArrayList<>();
        songList.add(new RelaxSong("Gnossienne No.1 – Erik Satie", 0, R.drawable.song1));
        songList.add(new RelaxSong("Gymnopédie No.1 – Erik Satie", 0, R.drawable.song2));
        songList.add(new RelaxSong("Fugue BWV 543 – Bach", 0, R.drawable.song3));
    }

    private void loadSong(int index) {
        retryCount = 0;
        loadSongWithRetry(index);
    }

    private void loadSongWithRetry(int index) {
        RelaxSong currentSong = songList.get(index);

        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText("מוזיקה מרגיעה 🎵");
        albumArt.setImageResource(currentSong.getImageResourceId());

        releasePlayer();

        isPrepared = false;
        isPlaying = false;
        btnPlayPause.setEnabled(false);
        btnNext.setEnabled(false);
        btnPrev.setEnabled(false);
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        seekBar.setProgress(0);

        Toast.makeText(this, "טוען שיר...", Toast.LENGTH_SHORT).show();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        try {
            mediaPlayer.setDataSource(URLS[index]);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                seekBar.setMax(mp.getDuration());
                btnPlayPause.setEnabled(true);
                btnNext.setEnabled(true);
                btnPrev.setEnabled(true);
                Toast.makeText(this, "מוכן — לחץ להפעלה ▶", Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                currentIndex = (currentIndex + 1) % songList.size();
                loadSong(currentIndex);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPrepared = false;
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    Toast.makeText(this, "בעיית רשת, מנסה שוב... (" + retryCount + "/" + MAX_RETRIES + ")", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(() -> loadSongWithRetry(index), 2000);
                } else {
                    Toast.makeText(this, "לא ניתן לטעון, מדלג לשיר הבא", Toast.LENGTH_LONG).show();
                    currentIndex = (currentIndex + 1) % songList.size();
                    loadSong(currentIndex);
                }
                return true;
            });

        } catch (Exception e) {
            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || !isPrepared) return;

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

    private void releasePlayer() {
        handler.removeCallbacks(updateSeekBar);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) togglePlayPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
}