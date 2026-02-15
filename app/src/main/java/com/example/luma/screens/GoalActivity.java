package com.example.luma.screens;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.luma.R;
import com.example.luma.models.Goal;
import com.example.luma.models.AlarmReceiver;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GoalActivity extends AppCompatActivity {

    private TextInputEditText editTitle;
    private TimePicker timePicker;
    private CheckBox checkSun, checkMon, checkTue, checkWed, checkThu, checkFri, checkSat;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        // --- בקשת הרשאה להתראות (חובה לאנדרואיד 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        createNotificationChannel();
        initViews();

        btnSave.setOnClickListener(v -> saveGoal());
    }

    private void initViews() {
        editTitle = findViewById(R.id.editTitle);
        timePicker = findViewById(R.id.timePicker);
        btnSave = findViewById(R.id.btnSave);
        checkSun = findViewById(R.id.checkSun);
        checkMon = findViewById(R.id.checkMon);
        checkTue = findViewById(R.id.checkTue);
        checkWed = findViewById(R.id.checkWed);
        checkThu = findViewById(R.id.checkThu);
        checkFri = findViewById(R.id.checkFri);
        checkSat = findViewById(R.id.checkSat);
        timePicker.setIs24HourView(true);
    }

    private void saveGoal() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            editTitle.setError("נא להזין שם למטרה");
            return;
        }

        List<String> selectedDays = new ArrayList<>();
        if (checkSun.isChecked()) selectedDays.add("ראשון");
        if (checkMon.isChecked()) selectedDays.add("שני");
        if (checkTue.isChecked()) selectedDays.add("שלישי");
        if (checkWed.isChecked()) selectedDays.add("רביעי");
        if (checkThu.isChecked()) selectedDays.add("חמישי");
        if (checkFri.isChecked()) selectedDays.add("שישי");
        if (checkSat.isChecked()) selectedDays.add("שבת");

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "בחר לפחות יום אחד", Toast.LENGTH_SHORT).show();
            return;
        }

        Goal newGoal = new Goal(String.valueOf(System.currentTimeMillis()),
                title, selectedDays,
                timePicker.getHour(), timePicker.getMinute());

        saveToPrefs(newGoal);
        scheduleNotifications(newGoal);

        Toast.makeText(this, "המטרה נשמרה ותוזמנה!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveToPrefs(Goal goal) {
        SharedPreferences prefs = getSharedPreferences("LumaData", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("goals_list", null);
        Type type = new TypeToken<ArrayList<Goal>>() {}.getType();
        List<Goal> goalList = gson.fromJson(json, type);
        if (goalList == null) goalList = new ArrayList<>();
        goalList.add(goal);
        prefs.edit().putString("goals_list", gson.toJson(goalList)).apply();
    }

    private void scheduleNotifications(Goal goal) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (String dayStr : goal.getDays()) {
            int dayNum = dayToNumber(dayStr);

            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.DAY_OF_WEEK, dayNum);
            calendar.set(Calendar.HOUR_OF_DAY, goal.getHour());
            calendar.set(Calendar.MINUTE, goal.getMinute());
            calendar.set(Calendar.SECOND, 0);

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("goalTitle", goal.getTitle());

            int requestCode = (goal.getId() + dayStr).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                // בבדיקה נשתמש ב-set ולא ב-setRepeating כדי שזה יקפוץ פעם אחת מיד
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    private int dayToNumber(String day) {
        switch (day) {
            case "ראשון": return Calendar.SUNDAY;
            case "שני": return Calendar.MONDAY;
            case "שלישי": return Calendar.TUESDAY;
            case "רביעי": return Calendar.WEDNESDAY;
            case "חמישי": return Calendar.THURSDAY;
            case "שישי": return Calendar.FRIDAY;
            case "שבת": return Calendar.SATURDAY;
            default: return Calendar.SUNDAY;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("LUMA_CHANNEL", "Luma Goals", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}