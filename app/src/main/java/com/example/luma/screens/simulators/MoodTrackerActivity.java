package com.example.luma.screens.simulators;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.luma.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.*;

public class MoodTrackerActivity extends AppCompatActivity {

    // UI Components
    private Button btnMood1, btnMood2, btnMood3, btnMood4, btnMood5;
    private EditText etNotes;
    private Button btnSubmit;
    private LineChart chartMood;
    private TextView tvEmoji1Count, tvEmoji2Count, tvEmoji3Count, tvEmoji4Count, tvEmoji5Count;

    // Data
    private Integer selectedMood = null;
    private List<MoodEntry> moodHistory = new ArrayList<>();

    // Colors
    private final int colorPrimary = 0xFF700053;
    private final int colorSecondary = 0xFFF7F3F0;
    private final int colorBorder = 0xFFE5DDD8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_tracker);

        initViews();
        setupMoodButtons();
        setupSubmitButton();
        loadMoodHistory();
        updateChart();
        updateEmojiSummary();
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
            int moodValue = i + 1;
            Button button = buttons[i];

            if (selectedMood != null && selectedMood == moodValue) {
                // Selected state
                button.setBackgroundColor(colorPrimary);
                button.setTextColor(colorSecondary);
                button.setElevation(8f);
                button.setScaleX(1.05f);
                button.setScaleY(1.05f);
            } else {
                // Unselected state
                button.setBackgroundColor(colorSecondary);
                button.setTextColor(colorPrimary);
                button.setElevation(0f);
                button.setScaleX(1.0f);
                button.setScaleY(1.0f);
            }
        }

        // Enable submit button if mood is selected
        btnSubmit.setEnabled(selectedMood != null);
        btnSubmit.setAlpha(selectedMood != null ? 1.0f : 0.6f);
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> submitMoodEntry());
    }

    private void submitMoodEntry() {
        if (selectedMood == null) return;

        String notes = etNotes.getText().toString();

        MoodEntry entry = new MoodEntry(
                UUID.randomUUID().toString(),
                getCurrentUserId(),
                new Date(),
                selectedMood,
                notes.isEmpty() ? null : notes
        );

        // Save to database/adapter
        saveMoodEntry(entry);

        // Add to history
        moodHistory.add(entry);

        // Update UI
        updateChart();
        updateEmojiSummary();

        // Reset form
        selectedMood = null;
        etNotes.setText("");
        updateMoodButtonsUI();

        // Show feedback
        Toast.makeText(this, "מצב הרוח נשמר בהצלחה! 💜", Toast.LENGTH_SHORT).show();

        // Animation feedback
        btnSubmit.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    btnSubmit.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void loadMoodHistory() {
        // Load from your adapter/database
        // Example:
        // moodHistory.addAll(moodAdapter.getUserMoods(getCurrentUserId()));

        // Demo data for testing
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 5; i >= 1; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            moodHistory.add(new MoodEntry(
                    UUID.randomUUID().toString(),
                    getCurrentUserId(),
                    calendar.getTime(),
                    new Random().nextInt(4) + 2, // Random 2-5
                    null
            ));
        }
    }

    private void updateChart() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < moodHistory.size(); i++) {
            entries.add(new Entry(i, moodHistory.get(i).moodValue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "מצב רוח");
        dataSet.setColor(colorPrimary);
        dataSet.setCircleColor(colorPrimary);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(0f);
        dataSet.setDrawFilled(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chartMood.setData(new LineData(dataSet));
        chartMood.getDescription().setEnabled(false);
        chartMood.getLegend().setEnabled(false);

        // X Axis
        XAxis xAxis = chartMood.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(colorPrimary);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(colorBorder);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < moodHistory.size()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                    return sdf.format(moodHistory.get(index).timestamp);
                }
                return "";
            }
        });

        // Y Axis
        chartMood.getAxisLeft().setTextColor(colorPrimary);
        chartMood.getAxisLeft().setAxisMinimum(0f);
        chartMood.getAxisLeft().setAxisMaximum(6f);
        chartMood.getAxisLeft().setDrawGridLines(true);
        chartMood.getAxisLeft().setGridColor(colorBorder);

        chartMood.getAxisRight().setEnabled(false);

        chartMood.setTouchEnabled(true);
        chartMood.setDragEnabled(true);
        chartMood.setScaleEnabled(false);
        chartMood.setPinchZoom(false);

        chartMood.animateX(1000);
        chartMood.invalidate();
    }

    private void updateEmojiSummary() {
        int[] counts = new int[6]; // 0-5, index 0 unused

        for (MoodEntry entry : moodHistory) {
            counts[entry.moodValue]++;
        }

        tvEmoji1Count.setText(counts[1] > 0 ? "×" + counts[1] : "");
        tvEmoji2Count.setText(counts[2] > 0 ? "×" + counts[2] : "");
        tvEmoji3Count.setText(counts[3] > 0 ? "×" + counts[3] : "");
        tvEmoji4Count.setText(counts[4] > 0 ? "×" + counts[4] : "");
        tvEmoji5Count.setText(counts[5] > 0 ? "×" + counts[5] : "");

        // Hide emoji summaries with 0 count
        tvEmoji1Count.setVisibility(counts[1] > 0 ? View.VISIBLE : View.GONE);
        tvEmoji2Count.setVisibility(counts[2] > 0 ? View.VISIBLE : View.GONE);
        tvEmoji3Count.setVisibility(counts[3] > 0 ? View.VISIBLE : View.GONE);
        tvEmoji4Count.setVisibility(counts[4] > 0 ? View.VISIBLE : View.GONE);
        tvEmoji5Count.setVisibility(counts[5] > 0 ? View.VISIBLE : View.GONE);
    }

    private void saveMoodEntry(MoodEntry entry) {
        // Save using your adapter
        // Example:
        // moodAdapter.insertMoodEntry(entry);
        // or
        // database.moodDao().insert(entry);
    }

    private String getCurrentUserId() {
        // Get from your User class/session
        // Example:
        // return User.getCurrentUser().getId();
        return "user_123"; // Placeholder
    }

    // Data class
    public static class MoodEntry {
        public String id;
        public String userId;
        public Date timestamp;
        public int moodValue; // 1-5
        public String notes;

        public MoodEntry(String id, String userId, Date timestamp, int moodValue, String notes) {
            this.id = id;
            this.userId = userId;
            this.timestamp = timestamp;
            this.moodValue = moodValue;
            this.notes = notes;
        }
    }
}