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

    // שימוש במודל שיצרת במקום מערכים מופרדים
    private List<RelaxSong> songList;
    private int currentIndex = 0;

    private MediaPlayer mediaPlayer;
    private FloatingActionButton btnPlayPause;
    private SeekBar seekBar;
    private TextView tvTitle, tvArtist;
    private ImageView albumArt;

    private boolean isPlaying = false;

    private final Handler handler = new Handler();
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        // הגדרת Padding אוטומטי כדי שהתוכן לא יוסתר על ידי ה-System Bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mediaPlayer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnPlayPause = findViewById(R.id.btn_play_pause);
        seekBar      = findViewById(R.id.seekbar);
        tvTitle      = findViewById(R.id.tv_title);
        tvArtist     = findViewById(R.id.tv_artist);
        albumArt     = findViewById(R.id.album_art);

        // אתחול רשימת השירים באמצעות המודל שלך
        initSongList();

        // טעינת השיר הראשון
        loadSong(currentIndex);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void initSongList() {
        songList = new ArrayList<>();

        // הערה: מכיוון שהמודל המקורי שלך מקבל int לקובץ השמע (עבור תיקיית raw),
        // זמנית נציב שם 0 ונשתמש בקישורים הקיימים, או שתוכל להחליף את המודל ל-String עבור URL.
        // כאן השתמשנו במודל שלך, ובזמן הטעינה נשלב את הקישורים שבחרת:
        songList.add(new RelaxSong("Gnossienne No.1 – Erik Satie", 0, android.R.drawable.ic_menu_gallery));
        songList.add(new RelaxSong("Gymnopédie No.1 – Erik Satie", 0, android.R.drawable.ic_menu_gallery));
        songList.add(new RelaxSong("Fugue BWV 543 – Bach", 0, android.R.drawable.ic_menu_gallery));
    }

    // מערך עזר זמני לקישורים בהתאמה לאינדקסים של המודל שלך
    private final String[] URLS = {
            "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/bd/Gnossienne_No_1.ogg/Gnossienne_No_1.ogg.mp3",
            "https://upload.wikimedia.org/wikipedia/commons/transcoded/6/6a/Erik_Satie_-_Gymnopedie_No._1.ogg/Erik_Satie_-_Gymnopedie_No._1.ogg.mp3",
            "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/4e/BWV_543-fugue.ogg/BWV_543-fugue.ogg.mp3"
    };

    private void loadSong(int index) {
        if (songList == null || songList.isEmpty()) return;

        RelaxSong currentSong = songList.get(index);

        // עדכון ממשק המשתמש מהמודל
        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText("מוזיקה מרגיעה 🎵");
        albumArt.setImageResource(currentSong.getImageResourceId());

        // עצירה וניקוי של הנגן הקודם
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.release();
            mediaPlayer = null;
        }

        isPlaying = false;
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        seekBar.setProgress(0); // איפוס הבר בתחילת שיר חדש

        Toast.makeText(this, "טוען שיר...", Toast.LENGTH_SHORT).show();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        try {
            // טעינה מהאינטרנט לפי האינדקס הנוכחי
            mediaPlayer.setDataSource(URLS[index]);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                Toast.makeText(this, "מוכן לנגינה ▶", Toast.LENGTH_SHORT).show();
                btnPlayPause.setEnabled(true);
                seekBar.setMax(mediaPlayer.getDuration()); // הגדרת אורך הבר המקסימלי
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                // מעבר אוטומטי לשיר הבא בלולאה קבועה
                currentIndex = (currentIndex + 1) % songList.size();
                loadSong(currentIndex);
                // הפעלה אוטומטית של השיר הבא
                togglePlayPause();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "שגיאה בטעינת השיר", Toast.LENGTH_SHORT).show();
                return true;
            });

            btnPlayPause.setEnabled(false);
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