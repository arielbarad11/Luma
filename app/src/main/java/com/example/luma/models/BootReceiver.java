package com.example.luma.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.luma.services.DatabaseService;
import com.example.luma.utils.NotificationHelper;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.List;

/**
 * BootReceiver - רכיב המופעל אוטומטית כשהטלפון נדלק (Reboot).
 * תפקידו לשחזר את כל התראות המטרות, כיוון שה-AlarmManager מתנקה בכל כיבוי של המכשיר.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // בדיקה שהאירוע שהתקבל הוא אכן סיום עליית המכשיר
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device rebooted, rescheduling alarms...");
            
            try {
                // שליפת מזהה המשתמש מה-SharedPreferences
                String userId = SharedPreferencesUtil.getUserId(context);
                if (userId != null) {
                    // משיכת רשימת המטרות העדכנית מהשרת
                    DatabaseService.getInstance().getGoals(userId, new DatabaseService.DatabaseCallback<List<Goal>>() {
                        @Override
                        public void onCompleted(List<Goal> goals) {
                            if (goals != null && !goals.isEmpty()) {
                                // תזמון מחדש של כל מטרה ומטרה
                                for (Goal goal : goals) {
                                    NotificationHelper.scheduleGoalNotifications(context, goal);
                                }
                                Log.d(TAG, "Successfully rescheduled " + goals.size() + " goals");
                            }
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Log.e(TAG, "Failed to fetch goals on boot", e);
                        }
                    });
                }
            } catch (Exception e) {
                // הגנה מפני קריסה במקרה של בעיית תאימות בנתונים (כמו JsonSyntaxException)
                Log.e(TAG, "Error in BootReceiver during data parsing", e);
            }
        }
    }
}