package com.example.luma.screens.simulators;

import android.graphics.Color;
import android.os.Bundle;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// יורש מ-BaseActivity כדי לקבל גישה ל-databaseService
public class MoodTrackerActivity extends BaseActivity {

    // שימוש בצבעי המותג מה-Theme
    private final int colorPrimary = Color.parseColor("#005396"); // כחול Luma
    private final int colorAccent = Color.parseColor("#700053");  // סגול לסימון
    private final int colorBackground = Color.parseColor("#F7F3F0"); // צבע חול
    private Button btnMood1, btnMood2, btnMood3, btnMood4, btnMood5;
    private EditText etNotes;
    private Button btnSubmit;
    private LineChart chartMood;
    private TextView tvEmoji1Count, tvEmoji2Count, tvEmoji3Count, tvEmoji4Count, tvEmoji5Count;
    private Integer selectedMood = null;
    private List<MoodEntry> moodHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mood_tracker);

        // יישור ל-Insets (Status Bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_mood_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupMoodButtons();
        setupSubmitButton();
        refreshData(); // טעינה ראשונית מה-DB
    }

    private void initViews() {
        btnMood1 = findViewById(R.id.btnMood1);
        btnMood2 = findViewById(R.id.btnMood2);
        btnMood3 = findViewById(R.id.btnMood3);
        btnMood4 = findViewById(R.id.btnMood4);
        btnMood5 = findViewById(R.id.btnMood5);
        etNotes = findViewById(R.id.etNotes);
        btnSubmit = findViewById(R.id.btnSubmit);
        chartMood = findViewById(R.id.chartMood);
        tvEmoji1Count = findViewById(R.id.tvEmoji1Count);
        tvEmoji2Count = findViewById(R.id.tvEmoji2Count);
        tvEmoji3Count = findViewById(R.id.tvEmoji3Count);
        tvEmoji4Count = findViewById(R.id.tvEmoji4Count);
        tvEmoji5Count = findViewById(R.id.tvEmoji5Count);
    }

    // פונקציה לטעינת נתונים אמיתיים מה-Firebase
    private void refreshData() {
        String userId = getCurrentUserId();
        // כאן את צריכה לוודא שיש לך פונקציה ב-DatabaseService שמושכת מעקבים
        databaseService.getMoodHistory(userId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<MoodEntry> entries) {
                moodHistory = entries;
                // מיון לפי תאריך כדי שהגרף ייראה נכון
                moodHistory.sort(Comparator.comparing(o -> o.timestamp));
                updateChart();
                updateEmojiSummary();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(MoodTrackerActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            if (selectedMood == null) return;

            MoodEntry entry = new MoodEntry(
                    UUID.randomUUID().toString(),
                    getCurrentUserId(),
                    new Date(),
                    selectedMood,
                    etNotes.getText().toString()
            );

            databaseService.saveMoodEntry(entry, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(Void result) {
                    Toast.makeText(MoodTrackerActivity.this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                    selectedMood = null;
                    etNotes.setText("");
                    updateMoodButtonsUI();
                    refreshData(); // עדכון הגרף מיד אחרי השמירה
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(MoodTrackerActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateChart() {
        if (moodHistory.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < moodHistory.size(); i++) {
            entries.add(new Entry(i, moodHistory.get(i).moodValue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "מצב רוח לאורך זמן");
        dataSet.setColor(colorPrimary);
        dataSet.setCircleColor(colorPrimary);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER); // גרף מעוגל ויפה

        chartMood.setData(new LineData(dataSet));
        chartMood.setBackgroundColor(colorBackground);
        chartMood.getDescription().setEnabled(false);

        XAxis xAxis = chartMood.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < moodHistory.size()) {
                    return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(moodHistory.get(idx).timestamp);
                }
                return "";
            }
        });

        chartMood.getAxisRight().setEnabled(false);
        chartMood.getAxisLeft().setAxisMinimum(1f);
        chartMood.getAxisLeft().setAxisMaximum(5f);
        chartMood.animateX(800);
        chartMood.invalidate();
    }

    private void updateEmojiSummary() {
        int[] counts = new int[6];
        for (MoodEntry entry : moodHistory) counts[entry.moodValue]++;

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