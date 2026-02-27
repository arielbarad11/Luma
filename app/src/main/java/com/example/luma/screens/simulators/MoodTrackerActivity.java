package com.example.luma.screens.simulators;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.MoodEntry;
import com.example.luma.models.User;
import com.example.luma.screens.BaseActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MoodTrackerActivity extends BaseActivity {

    private final int colorPrimary    = Color.parseColor("#005396");
    private final int colorAccent     = Color.parseColor("#700053");
    private final int colorBackground = Color.parseColor("#F7F3F0");

    private Button   btnMood1, btnMood2, btnMood3, btnMood4, btnMood5;
    private EditText etNotes;
    private Button   btnSubmit;
    private LineChart chartMood;
    private TextView tvEmoji1Count, tvEmoji2Count, tvEmoji3Count, tvEmoji4Count, tvEmoji5Count;
    private TextView tvAlreadySubmitted; // טקסט אופציונלי שמודיע למשתמש

    private Integer        selectedMood = null;
    private List<MoodEntry> moodHistory = new ArrayList<>();

    // ───────────── Lifecycle ─────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mood_tracker);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_mood_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupMoodButtons();
        setupSubmitButton();
        setupChartClickListener(); // לחיצה על הגרף -> מסך מלא
        refreshData();
    }

    // ───────────── Init ─────────────

    private void initViews() {
        btnMood1 = findViewById(R.id.btnMood1);
        btnMood2 = findViewById(R.id.btnMood2);
        btnMood3 = findViewById(R.id.btnMood3);
        btnMood4 = findViewById(R.id.btnMood4);
        btnMood5 = findViewById(R.id.btnMood5);
        etNotes  = findViewById(R.id.etNotes);
        btnSubmit = findViewById(R.id.btnSubmit);
        chartMood = findViewById(R.id.chartMood);

        tvEmoji1Count = findViewById(R.id.tvEmoji1Count);
        tvEmoji2Count = findViewById(R.id.tvEmoji2Count);
        tvEmoji3Count = findViewById(R.id.tvEmoji3Count);
        tvEmoji4Count = findViewById(R.id.tvEmoji4Count);
        tvEmoji5Count = findViewById(R.id.tvEmoji5Count);

        // אם יש לך TextView לכך ב-XML – אחרת אפשר להסיר
        // tvAlreadySubmitted = findViewById(R.id.tvAlreadySubmitted);
    }

    // ───────────── Firebase ─────────────

    private void refreshData() {
        String userId = getCurrentUserId();
        databaseService.getMoodHistory(userId, new DatabaseService.DatabaseCallback<List<MoodEntry>>() {
            @Override
            public void onCompleted(List<MoodEntry> entries) {
                moodHistory = entries;
                moodHistory.sort(Comparator.comparing(o -> o.timestamp));
                updateChart();
                updateEmojiSummary();
                updateInputAreaForToday(); // ← בדיקה אם כבר הוגש היום
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(MoodTrackerActivity.this,
                        "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ───────────── One mood per day ─────────────

    /**
     * בודק אם קיים כניסה שנוצרה היום.
     * אם כן – מנטרל את אזור ההגשה ומציג הודעה.
     */
    private void updateInputAreaForToday() {
        boolean submittedToday = hasSubmittedToday();

        btnMood1.setEnabled(!submittedToday);
        btnMood2.setEnabled(!submittedToday);
        btnMood3.setEnabled(!submittedToday);
        btnMood4.setEnabled(!submittedToday);
        btnMood5.setEnabled(!submittedToday);
        etNotes.setEnabled(!submittedToday);
        btnSubmit.setEnabled(!submittedToday);

        if (submittedToday) {
            // אפשר גם להציג טקסט אם יש לך את ה-View ב-XML
            Toast.makeText(this,
                    "כבר הגשת מצב רוח היום 😊 חזור מחר!",
                    Toast.LENGTH_LONG).show();
        }
    }

    /** מחזיר true אם יש לפחות רשומה אחת שה-timestamp שלה הוא היום */
    private boolean hasSubmittedToday() {
        Calendar today = Calendar.getInstance();
        for (MoodEntry entry : moodHistory) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(entry.timestamp);
            if (cal.get(Calendar.YEAR)         == today.get(Calendar.YEAR)  &&
                    cal.get(Calendar.DAY_OF_YEAR)  == today.get(Calendar.DAY_OF_YEAR)) {
                return true;
            }
        }
        return false;
    }

    // ───────────── Mood Buttons ─────────────

    private void setupMoodButtons() {
        Button[] buttons = {btnMood1, btnMood2, btnMood3, btnMood4, btnMood5};
        for (int i = 0; i < buttons.length; i++) {
            final int moodValue = i + 1;
            buttons[i].setOnClickListener(v -> {
                selectedMood = moodValue;
                updateMoodButtonsUI();
            });
        }
    }

    private void updateMoodButtonsUI() {
        Button[] buttons = {btnMood1, btnMood2, btnMood3, btnMood4, btnMood5};
        for (int i = 0; i < buttons.length; i++) {
            if (selectedMood != null && selectedMood == (i + 1)) {
                buttons[i].setBackgroundColor(colorAccent);
                buttons[i].setScaleX(1.1f);
                buttons[i].setScaleY(1.1f);
            } else {
                buttons[i].setBackgroundColor(Color.TRANSPARENT);
                buttons[i].setScaleX(1.0f);
                buttons[i].setScaleY(1.0f);
            }
        }
        btnSubmit.setEnabled(selectedMood != null);
    }

    // ───────────── Submit ─────────────

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (selectedMood == null) return;

            // הגנה כפולה – פעם נוספת ב-client לפני שמירה
            if (hasSubmittedToday()) {
                Toast.makeText(this,
                        "כבר הגשת מצב רוח היום!", Toast.LENGTH_SHORT).show();
                return;
            }

            MoodEntry entry = new MoodEntry(
                    UUID.randomUUID().toString(),
                    getCurrentUserId(),
                    new Date(),
                    selectedMood,
                    etNotes.getText().toString()
            );

            databaseService.saveMoodEntry(entry, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void result) {
                    Toast.makeText(MoodTrackerActivity.this,
                            "נשמר בהצלחה ✅", Toast.LENGTH_SHORT).show();
                    selectedMood = null;
                    etNotes.setText("");
                    updateMoodButtonsUI();
                    refreshData(); // יטען מחדש ויחסום את האזור
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(MoodTrackerActivity.this,
                            "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ───────────── Chart ─────────────

    private void updateChart() {
        if (moodHistory.isEmpty()) return;
        configureChart(chartMood, moodHistory);
    }

    /**
     * מגדיר LineChart נתון עם הנתונים מה-moodHistory.
     * משמש גם לגרף הראשי וגם לגרף שבדיאלוג המסך המלא.
     */
    private void configureChart(LineChart chart, List<MoodEntry> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i).moodValue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "מצב רוח לאורך זמן");
        dataSet.setColor(colorPrimary);
        dataSet.setCircleColor(colorPrimary);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        chart.setData(new LineData(dataSet));
        chart.setBackgroundColor(colorBackground);
        chart.getDescription().setEnabled(false);

        // ──── גלילה (Scrolling / Drag) ────
        chart.setDragEnabled(true);           // גרירה שמאלה/ימינה
        chart.setScaleXEnabled(true);         // זום ציר X
        chart.setScaleYEnabled(false);        // ללא זום ציר Y
        chart.setPinchZoom(false);

        // הצגת חלון של 10 נקודות בו-זמנית; אם יש פחות מ-10 נראה הכול
        float visibleCount = Math.min(10f, data.size());
        if (visibleCount > 0) chart.setVisibleXRangeMaximum(visibleCount);

        // גלול לסוף (הכי עדכני)
        chart.moveViewToX(data.size() - 1);

        // ──── ציר X ────
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < data.size()) {
                    return new SimpleDateFormat("dd/MM", Locale.getDefault())
                            .format(data.get(idx).timestamp);
                }
                return "";
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(1f);
        chart.getAxisLeft().setAxisMaximum(5f);

        chart.animateX(800);
        chart.invalidate();
    }

    // ───────────── Fullscreen chart on tap ─────────────

    private void setupChartClickListener() {
        chartMood.setOnClickListener(v -> openFullscreenChart());
    }

    private void openFullscreenChart() {
        if (moodHistory.isEmpty()) return;

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_chart);

        // חשוב: ודאי שב-XML יש LineChart עם id fullscreenChart ו-Button עם id btnCloseChart
        LineChart fullChart = dialog.findViewById(R.id.fullscreenChart);
        Button btnClose     = dialog.findViewById(R.id.btnCloseChart);

        configureChart(fullChart, moodHistory);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }
        dialog.show();
    }

    // ───────────── Emoji Summary ─────────────

    private void updateEmojiSummary() {
        int[] counts = new int[6];
        for (MoodEntry entry : moodHistory) {
            if (entry.moodValue >= 1 && entry.moodValue <= 5) {
                counts[entry.moodValue]++;
            }
        }
        tvEmoji1Count.setText(MessageFormat.format("×{0}", counts[1]));
        tvEmoji2Count.setText(MessageFormat.format("×{0}", counts[2]));
        tvEmoji3Count.setText(MessageFormat.format("×{0}", counts[3]));
        tvEmoji4Count.setText(MessageFormat.format("×{0}", counts[4]));
        tvEmoji5Count.setText(MessageFormat.format("×{0}", counts[5]));
    }

    // ───────────── Helpers ─────────────

    private String getCurrentUserId() {
        User user = SharedPreferencesUtil.getUser(this);
        return (user != null) ? user.getId() : "guest";
    }
}