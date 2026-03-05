package com.example.luma.screens;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.luma.R;
import com.example.luma.models.AlarmReceiver;
import com.example.luma.models.Goal;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GoalActivity extends AppCompatActivity {

    private TextInputEditText editTitle;
    private TimePicker timePicker;
    private CheckBox checkSun, checkMon, checkTue, checkWed, checkThu, checkFri, checkSat;
    private MaterialButton btnSave;
    private LinearLayout goalsContainer;
    private TextView tvEmptyGoals;

    private Goal editingGoal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        checkNotificationPermission();
        createNotificationChannel();
        initViews();
        btnSave.setOnClickListener(v -> saveGoal());
        loadExistingGoals();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initViews() {
        editTitle      = findViewById(R.id.editTitle);
        timePicker     = findViewById(R.id.timePicker);
        btnSave        = findViewById(R.id.btnSave);
        checkSun       = findViewById(R.id.checkSun);
        checkMon       = findViewById(R.id.checkMon);
        checkTue       = findViewById(R.id.checkTue);
        checkWed       = findViewById(R.id.checkWed);
        checkThu       = findViewById(R.id.checkThu);
        checkFri       = findViewById(R.id.checkFri);
        checkSat       = findViewById(R.id.checkSat);
        goalsContainer = findViewById(R.id.goalsContainer);
        tvEmptyGoals   = findViewById(R.id.tvEmptyGoals);
        timePicker.setIs24HourView(true);
    }

    private void loadExistingGoals() {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) return;

        DatabaseService.getInstance().getGoals(userId, new DatabaseService.DatabaseCallback<List<Goal>>() {
            @Override
            public void onCompleted(List<Goal> goals) {
                goalsContainer.removeAllViews();
                if (goals == null || goals.isEmpty()) {
                    tvEmptyGoals.setVisibility(View.VISIBLE);
                    return;
                }
                tvEmptyGoals.setVisibility(View.GONE);
                for (Goal goal : goals) {
                    addGoalCard(goal);
                }
            }

            @Override
            public void onFailed(Exception e) {
                tvEmptyGoals.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addGoalCard(Goal goal) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_goal_inline, goalsContainer, false);

        TextView tvTitle = card.findViewById(R.id.itemGoalTitle);
        TextView tvDays  = card.findViewById(R.id.itemGoalDays);
        TextView tvTime  = card.findViewById(R.id.itemGoalTime);
        MaterialButton btnEdit   = card.findViewById(R.id.btnEditGoal);
        MaterialButton btnDelete = card.findViewById(R.id.btnDeleteGoal);

        tvTitle.setText(goal.getTitle());
        tvDays.setText(String.join(" | ", goal.getDays()));
        tvTime.setText(goal.getFormattedTime());

        btnEdit.setOnClickListener(v -> fillFormForEdit(goal));
        btnDelete.setOnClickListener(v -> confirmDelete(goal));

        goalsContainer.addView(card);
    }

    private void fillFormForEdit(Goal goal) {
        editingGoal = goal;
        editTitle.setText(goal.getTitle());
        timePicker.setHour(goal.getHour());
        timePicker.setMinute(goal.getMinute());

        checkSun.setChecked(goal.getDays().contains("ראשון"));
        checkMon.setChecked(goal.getDays().contains("שני"));
        checkTue.setChecked(goal.getDays().contains("שלישי"));
        checkWed.setChecked(goal.getDays().contains("רביעי"));
        checkThu.setChecked(goal.getDays().contains("חמישי"));
        checkFri.setChecked(goal.getDays().contains("שישי"));
        checkSat.setChecked(goal.getDays().contains("שבת"));

        btnSave.setText("💾 עדכן מטרה");
        editTitle.requestFocus();
    }

    private void confirmDelete(Goal goal) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת מטרה")
                .setMessage("למחוק את \"" + goal.getTitle() + "\"?")
                .setPositiveButton("מחק", (d, w) -> deleteGoal(goal))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteGoal(Goal goal) {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) return;

        cancelNotifications(goal);

        DatabaseService.getInstance().deleteGoal(userId, goal.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(GoalActivity.this, "המטרה נמחקה", Toast.LENGTH_SHORT).show();
                loadExistingGoals();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GoalActivity.this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGoal() {
        // בדיקת הרשאת התראות מדויקות (Exact Alarm) לפני השמירה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog();
                return;
            }
        }

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

        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) return;

        String goalId = (editingGoal != null) ? editingGoal.getId() : String.valueOf(System.currentTimeMillis());
        if (editingGoal != null) cancelNotifications(editingGoal);

        Goal goal = new Goal(goalId, title, selectedDays, timePicker.getHour(), timePicker.getMinute());
        btnSave.setEnabled(false);

        DatabaseService.getInstance().saveGoal(userId, goal, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                scheduleNotifications(goal);
                Toast.makeText(GoalActivity.this, editingGoal != null ? "המטרה עודכנה!" : "המטרה נשמרה ותוזמנה!", Toast.LENGTH_SHORT).show();
                resetForm();
                loadExistingGoals();
                btnSave.setEnabled(true);
            }

            @Override
            public void onFailed(Exception e) {
                btnSave.setEnabled(true);
                Toast.makeText(GoalActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אישור התראות מדויקות")
                .setMessage("כדי שהאפליקציה תוכל לשלוח התראות בזמן המדויק, יש לאשר לה 'התראות ותזכורות' בהגדרות המערכת.")
                .setPositiveButton("מעבר להגדרות", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void resetForm() {
        editingGoal = null;
        editTitle.setText("");
        timePicker.setHour(8);
        timePicker.setMinute(0);
        checkSun.setChecked(false);
        checkMon.setChecked(false);
        checkTue.setChecked(false);
        checkWed.setChecked(false);
        checkThu.setChecked(false);
        checkFri.setChecked(false);
        checkSat.setChecked(false);
        btnSave.setText("💾 שמור מטרה");
    }

    private void scheduleNotifications(Goal goal) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (String dayStr : goal.getDays()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, dayToNumber(dayStr));
            calendar.set(Calendar.HOUR_OF_DAY, goal.getHour());
            calendar.set(Calendar.MINUTE, goal.getMinute());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (!calendar.after(Calendar.getInstance())) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("goalTitle", goal.getTitle());

            int requestCode = (goal.getId() + dayStr).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } catch (SecurityException e) {
                // הגנה נוספת מפני קריסה אם המשתמש ביטל את ההרשאה בדיוק ברגע זה
                Toast.makeText(this, "חסרה הרשאה להתראות מדויקות", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void cancelNotifications(Goal goal) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (String dayStr : goal.getDays()) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            int requestCode = (goal.getId() + dayStr).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
    }

    private int dayToNumber(String day) {
        switch (day) {
            case "ראשון":  return Calendar.SUNDAY;
            case "שני":    return Calendar.MONDAY;
            case "שלישי":  return Calendar.TUESDAY;
            case "רביעי":  return Calendar.WEDNESDAY;
            case "חמישי":  return Calendar.THURSDAY;
            case "שישי":   return Calendar.FRIDAY;
            case "שבת":    return Calendar.SATURDAY;
            default:       return Calendar.SUNDAY;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "LUMA_CHANNEL", "Luma Goals", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}