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

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * MoodTrackerActivity - מערכת למעקב אחר מצב הרוח היומי.
 * המערכת מאפשרת למשתמש לבחור אימוג'י המייצג את הרגשתו, להוסיף הערה ולראות גרף התקדמות.
 */
public class MoodTrackerActivity extends BaseActivity {

    // צבעים מוגדרים מראש לעיצוב הגרף והכפתורים
    private final int colorPrimary    = Color.parseColor("#005396");
    private final int colorAccent     = Color.parseColor("#700053");
    private final int colorBackground = Color.parseColor("#F7F3F0");

    // רכיבי ה-UI
    private Button btnMood1, btnMood2, btnMood3, btnMood4, btnMood5;
    private EditText etNotes;
    private Button btnSubmit;
    private LineChart chartMood; // הגרף המציג את היסטוריית מצבי הרוח
    private TextView tvEmoji1Count, tvEmoji2Count, tvEmoji3Count, tvEmoji4Count, tvEmoji5Count;

    private Integer selectedMood = null; // משתנה לשמירת הבחירה הנוכחית של המשתמש (1-5)
    private List<MoodEntry> moodHistory = new ArrayList<>(); // רשימת כל הכניסות הקודמות מהשרת

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mood_tracker);

        // הגדרת Padding אוטומטי למניעת חפיפה עם שולי המסך
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_mood_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews(); // אתחול הרכיבים
        setupMoodButtons(); // הגדרת כפתורי האימוג'ים
        setupSubmitButton(); // הגדרת כפתור השליחה
        setupChartClickListener(); // מאזין ללחיצה על הגרף להגדלה
        refreshData(); // טעינת נתונים ראשונית מה-Firebase
    }

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
    }

    /**
     * טעינת היסטוריית מצב הרוח מה-Firebase ועדכון הגרף והסיכום.
     */
    private void refreshData() {
        String userId = getCurrentUserId();
        databaseService.getMoodHistory(userId, new DatabaseService.DatabaseCallback<List<MoodEntry>>() {
            @Override
            public void onCompleted(List<MoodEntry> entries) {
                moodHistory = entries;
                // מיון הרשימה לפי תאריך כדי שהגרף יוצג נכון משמאל לימין
                moodHistory.sort(Comparator.comparing(o -> o.timestamp));
                updateChart(); // עדכון הגרף
                updateEmojiSummary(); // עדכון המונים של כל אימוג'י
                updateInputAreaForToday(); // בדיקה אם המשתמש כבר הגיש היום
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(MoodTrackerActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * הגבלת הגשה: בודק אם המשתמש כבר הגיש מצב רוח היום.
     * אם כן - נועל את כפתורי הבחירה כדי למנוע הצפה של נתונים.
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
            Toast.makeText(this, "כבר עדכנת מצב רוח היום 😊 נתראה מחר!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * בדיקה לוגית באמצעות Calendar האם קיימת רשומה מהתאריך של היום.
     */
    private boolean hasSubmittedToday() {
        Calendar today = Calendar.getInstance();
        for (MoodEntry entry : moodHistory) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(entry.timestamp);
            if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * הגדרת מאזינים לכפתורי האימוג'ים ושינוי צבע הכפתור שנבחר.
     */
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
                buttons[i].setBackgroundColor(colorAccent); // הדגשת הכפתור הנבחר
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

    /**
     * שליחת מצב הרוח ל-Firebase.
     */
    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (selectedMood == null || hasSubmittedToday()) return;

            MoodEntry entry = new MoodEntry(
                    UUID.randomUUID().toString(), // יצירת מזהה ייחודי לרשומה
                    getCurrentUserId(),
                    new Date(), // זמן נוכחי
                    selectedMood,
                    etNotes.getText().toString()
            );

            databaseService.saveMoodEntry(entry, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void result) {
                    Toast.makeText(MoodTrackerActivity.this, "נשמר בהצלחה ✅", Toast.LENGTH_SHORT).show();
                    resetInputFields();
                    refreshData(); // טעינה מחדש של הגרף ונעילת הכפתורים
                }
                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(MoodTrackerActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void resetInputFields() {
        selectedMood = null;
        etNotes.setText("");
        updateMoodButtonsUI();
    }

    /**
     * עדכון הגרף באמצעות ספריית MPAndroidChart.
     */
    private void updateChart() {
        if (moodHistory.isEmpty()) return;
        configureChart(chartMood, moodHistory);
    }

    /**
     * הגדרת הגרף (LineChart) - כולל עיצוב הצירים, גלילה ואנימציה.
     */
    private void configureChart(LineChart chart, List<MoodEntry> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i).moodValue)); // X=מיקום ברשימה, Y=ערך מצב הרוח
        }

        LineDataSet dataSet = new LineDataSet(entries, "מצב רוח לאורך זמן");
        dataSet.setColor(colorPrimary);
        dataSet.setCircleColor(colorPrimary);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER); // גרף מעוגל ויפה יותר

        chart.setData(new LineData(dataSet));
        chart.setBackgroundColor(colorBackground);
        chart.getDescription().setEnabled(false);

        // הגדרת גלילה בגרף (Scrolling)
        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);
        
        // הצגת מקסימום 10 נקודות בו-זמנית בגרף וגלילה לסוף (הכי עדכני)
        float visibleCount = Math.min(10f, data.size());
        if (visibleCount > 0) chart.setVisibleXRangeMaximum(visibleCount);
        chart.moveViewToX(data.size() - 1);

        // עיצוב ציר X (תאריכים)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < data.size()) {
                    return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(data.get(idx).timestamp);
                }
                return "";
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(1f);
        chart.getAxisLeft().setAxisMaximum(5f);
        chart.animateX(800);
        chart.invalidate(); // רענון הגרף
    }

    /**
     * לחיצה על הגרף הקטן פותחת דיאלוג עם הגרף במסך מלא.
     */
    private void setupChartClickListener() {
        chartMood.setOnClickListener(v -> openFullscreenChart());
    }

    private void openFullscreenChart() {
        if (moodHistory.isEmpty()) return;

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_full_graph);

        LineChart fullChart = dialog.findViewById(R.id.fullscreenChart);
        Button btnClose = dialog.findViewById(R.id.btnCloseChart);

        configureChart(fullChart, moodHistory); // שימוש באותה פונקציית הגדרת גרף

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * סיכום סטטיסטי: ספירה כמה פעמים המשתמש בחר כל אימוג'י.
     */
    private void updateEmojiSummary() {
        int[] counts = new int[6];
        for (MoodEntry entry : moodHistory) {
            if (entry.moodValue >= 1 && entry.moodValue <= 5) counts[entry.moodValue]++;
        }
        tvEmoji1Count.setText(MessageFormat.format("×{0}", counts[1]));
        tvEmoji2Count.setText(MessageFormat.format("×{0}", counts[2]));
        tvEmoji3Count.setText(MessageFormat.format("×{0}", counts[3]));
        tvEmoji4Count.setText(MessageFormat.format("×{0}", counts[4]));
        tvEmoji5Count.setText(MessageFormat.format("×{0}", counts[5]));
    }

    private String getCurrentUserId() {
        User user = SharedPreferencesUtil.getUser(this);
        return (user != null) ? user.getId() : "guest";
    }
}