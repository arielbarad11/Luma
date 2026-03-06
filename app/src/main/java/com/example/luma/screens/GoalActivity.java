package com.example.luma.screens;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.example.luma.models.Goal;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.NotificationHelper;
import com.example.luma.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך ניהול מטרות ותזכורות.
 * מאפשר למשתמש להוסיף, לערוך ולמחוק מטרות יומיות עם תזמון התראות.
 */
public class GoalActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש (UI)
    private TextInputEditText editTitle;
    private TimePicker timePicker;
    private CheckBox checkSun, checkMon, checkTue, checkWed, checkThu, checkFri, checkSat;
    private MaterialButton btnSave;
    private LinearLayout goalsContainer;
    private TextView tvEmptyGoals;

    // רשימה מקומית לשמירת המטרות שנטענו מהשרת - משמשת לבדיקת חפיפות בזמן אמת
    private List<Goal> currentGoalsList = new ArrayList<>();
    
    // אובייקט השומר את המטרה שבמצב עריכה (null אם זו מטרה חדשה)
    private Goal editingGoal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        // בדיקה וביקוש הרשאות התראה (נדרש ב-Android 13+)
        checkNotificationPermission();
        
        // יצירת ערוץ התראות (נדרש ב-Android 8+)
        createNotificationChannel();
        
        // אתחול רכיבי המסך
        initViews();
        
        // הגדרת מאזין ללחיצה על כפתור השמירה - מבצע וולידציה לפני שמירה
        btnSave.setOnClickListener(v -> validateAndSave());
        
        // טעינת המטרות הקיימות מה-Firebase
        loadExistingGoals();
    }

    /**
     * בדיקת הרשאות לשליחת התראות (עבור גרסאות אנדרואיד חדשות).
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    /**
     * קישור משתני הקוד לרכיבי ה-XML.
     */
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
        timePicker.setIs24HourView(true); // הגדרת שעון בפורמט 24 שעות
    }

    /**
     * טעינת המטרות של המשתמש מה-Database.
     */
    private void loadExistingGoals() {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) return;

        DatabaseService.getInstance().getGoals(userId, new DatabaseService.DatabaseCallback<List<Goal>>() {
            @Override
            public void onCompleted(List<Goal> goals) {
                goalsContainer.removeAllViews(); // ניקוי התצוגה לפני טעינה מחדש
                currentGoalsList = (goals != null) ? goals : new ArrayList<>();
                
                if (currentGoalsList.isEmpty()) {
                    tvEmptyGoals.setVisibility(View.VISIBLE);
                    return;
                }
                tvEmptyGoals.setVisibility(View.GONE);
                // הוספת כרטיס לכל מטרה ברשימה
                for (Goal goal : currentGoalsList) {
                    addGoalCard(goal);
                }
            }

            @Override
            public void onFailed(Exception e) {
                tvEmptyGoals.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * יצירה והוספה של כרטיס מטרה לתצוגה באופן דינמי.
     */
    private void addGoalCard(Goal goal) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_goal_inline, goalsContainer, false);
        ((TextView)card.findViewById(R.id.itemGoalTitle)).setText(goal.getTitle());
        ((TextView)card.findViewById(R.id.itemGoalDays)).setText(String.join(" | ", goal.getDays()));
        ((TextView)card.findViewById(R.id.itemGoalTime)).setText(goal.getFormattedTime());

        // הגדרת כפתורי עריכה ומחיקה בתוך הכרטיס
        card.findViewById(R.id.btnEditGoal).setOnClickListener(v -> fillFormForEdit(goal));
        card.findViewById(R.id.btnDeleteGoal).setOnClickListener(v -> confirmDelete(goal));
        
        goalsContainer.addView(card);
    }

    /**
     * מעבר למצב עריכה: מילוי הטופס בנתוני מטרה קיימת.
     */
    private void fillFormForEdit(Goal goal) {
        editingGoal = goal;
        editTitle.setText(goal.getTitle());
        timePicker.setHour(goal.getHour());
        timePicker.setMinute(goal.getMinute());
        
        // סימון הצ'קבוקסים לפי הימים שנבחרו
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

    /**
     * דיאלוג אישור לפני מחיקת מטרה.
     */
    private void confirmDelete(Goal goal) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת מטרה")
                .setMessage("למחוק את \"" + goal.getTitle() + "\"?")
                .setPositiveButton("מחק", (d, w) -> deleteGoal(goal))
                .setNegativeButton("ביטול", null).show();
    }

    /**
     * מחיקת מטרה מהשרת וביטול ההתראות שלה.
     */
    private void deleteGoal(Goal goal) {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) return;
        
        // ביטול התראות במערכת ההפעלה דרך ה-Helper
        NotificationHelper.cancelGoalNotifications(this, goal);
        
        DatabaseService.getInstance().deleteGoal(userId, goal.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                loadExistingGoals(); // טעינה מחדש של הרשימה המעודכנת
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GoalActivity.this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * פונקציית הליבה: וולידציה ובדיקת חפיפות לפני שמירה.
     */
    private void validateAndSave() {
        // בדיקת הרשאת "התראות מדויקות" (נדרש ב-Android 12+)
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

        List<String> selectedDays = getSelectedDays();
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "בחר לפחות יום אחד", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        // בדיקת חפיפה מול מטרות קיימות
        boolean hasExactConflict = false; // חפיפה מלאה (יום ושעה)
        boolean hasDayConflict = false;   // חפיפה חלקית (אותו יום)

        for (Goal existing : currentGoalsList) {
            // אם אנחנו בעריכה, נתעלם מהמטרה שאנחנו עורכים כרגע
            if (editingGoal != null && existing.getId().equals(editingGoal.getId())) continue;

            for (String day : selectedDays) {
                if (existing.getDays().contains(day)) {
                    hasDayConflict = true; // נמצאה מטרה אחרת באותו יום
                    if (existing.getHour() == hour && existing.getMinute() == minute) {
                        hasExactConflict = true; // נמצאה מטרה בדיוק באותה שעה
                        break;
                    }
                }
            }
            if (hasExactConflict) break;
        }

        // טיפול בחפיפה מלאה - חסימת שמירה
        if (hasExactConflict) {
            new AlertDialog.Builder(this)
                    .setTitle("שגיאה: התראה קיימת")
                    .setMessage("כבר יש לך מטרה מוגדרת בדיוק באותו יום ובאותה שעה.")
                    .setPositiveButton("הבנתי", null).show();
            return;
        }

        // טיפול בחפיפה יומית - אזהרה למשתמש
        if (hasDayConflict) {
            new AlertDialog.Builder(this)
                    .setTitle("התראה קיימת באותו יום")
                    .setMessage("המערכת זיהתה שיש לך כבר התראה לאותו היום, תרצה להוסיף עוד אחת?")
                    .setPositiveButton("כן, הוסף", (d, w) -> proceedToSave(title, selectedDays, hour, minute))
                    .setNegativeButton("לא, בטל", null).show();
        } else {
            proceedToSave(title, selectedDays, hour, minute); // אין חפיפה, שמירה ישירה
        }
    }

    /**
     * ביצוע השמירה בפועל ב-Firebase ותזמון ההתראה.
     */
    private void proceedToSave(String title, List<String> selectedDays, int hour, int minute) {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) return;

        // אם זה עדכון, נבטל קודם את ההתראות הישנות
        if (editingGoal != null) NotificationHelper.cancelGoalNotifications(this, editingGoal);

        String goalId = (editingGoal != null) ? editingGoal.getId() : String.valueOf(System.currentTimeMillis());
        Goal goal = new Goal(goalId, title, selectedDays, hour, minute);
        
        btnSave.setEnabled(false); // מניעת לחיצות כפולות בזמן השמירה
        
        DatabaseService.getInstance().saveGoal(userId, goal, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                // תזמון ההתראה החדשה במערכת ההפעלה
                NotificationHelper.scheduleGoalNotifications(GoalActivity.this, goal);
                resetForm(); // איפוס הטופס
                loadExistingGoals(); // רענון הרשימה
                btnSave.setEnabled(true);
            }
            @Override
            public void onFailed(Exception e) {
                btnSave.setEnabled(true);
                Toast.makeText(GoalActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * איסוף רשימת הימים שנבחרו על ידי המשתמש.
     */
    private List<String> getSelectedDays() {
        List<String> days = new ArrayList<>();
        if (checkSun.isChecked()) days.add("ראשון");
        if (checkMon.isChecked()) days.add("שני");
        if (checkTue.isChecked()) days.add("שלישי");
        if (checkWed.isChecked()) days.add("רביעי");
        if (checkThu.isChecked()) days.add("חמישי");
        if (checkFri.isChecked()) days.add("שישי");
        if (checkSat.isChecked()) days.add("שבת");
        return days;
    }

    /**
     * דיאלוג המפנה להגדרות המערכת לאישור הרשאת "התראות מדויקות".
     */
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
                }).setNegativeButton("ביטול", null).show();
    }

    /**
     * ניקוי הטופס וחזרה למצב הוספה חדשה.
     */
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

    /**
     * יצירת ערוץ התראות. חובה באנדרואיד 8 ומעלה כדי שהתראות יופיעו.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("LUMA_CHANNEL", "Luma Goals", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}