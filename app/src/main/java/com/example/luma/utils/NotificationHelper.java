package com.example.luma.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.luma.models.AlarmReceiver;
import com.example.luma.models.Goal;

import java.util.Calendar;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    public static void scheduleGoalNotifications(Context context, Goal goal) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (String dayStr : goal.getDays()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, goal.getHour());
            calendar.set(Calendar.MINUTE, goal.getMinute());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.DAY_OF_WEEK, dayToCalendarDay(dayStr));

            // אם הזמן כבר עבר היום, תזמן לשבוע הבא
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 7);
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("goalTitle", goal.getTitle());
            intent.putExtra("goalId", goal.getId());

            // מזהה ייחודי לכל יום של כל מטרה
            int requestCode = (goal.getId() + dayStr).hashCode();
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    requestCode, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
                Log.d(TAG, "Scheduled alarm for: " + goal.getTitle() + " on " + dayStr);
            } catch (Exception e) {
                Log.e(TAG, "Error scheduling alarm", e);
            }
        }
    }

    public static void cancelGoalNotifications(Context context, Goal goal) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (String dayStr : goal.getDays()) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            int requestCode = (goal.getId() + dayStr).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
    }

    private static int dayToCalendarDay(String day) {
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
}