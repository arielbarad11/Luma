package com.example.luma.models;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.luma.R;
import com.example.luma.screens.MainActivity;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("goalTitle");
        if (title == null) title = "המטרה שלך";
        
        Log.d(TAG, "Notification received for goal: " + title);

        // יצירת Intent לפתיחת האפליקציה כשלוחצים על ההתראה
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) System.currentTimeMillis(), 
                mainIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "LUMA_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // כדאי להחליף לאייקון האפליקציה שלך
                .setContentTitle("לומא - הגיע הזמן!")
                .setContentText("אל תשכח את המטרה שלך: " + title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // בדיקת הרשאה נוספת (ליתר ביטחון)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing POST_NOTIFICATIONS permission");
                    return;
                }
            }
            
            // שימוש ב-Hash של הכותרת כדי למנוע הצפה של אותה התראה
            int notificationId = title.hashCode();
            notificationManager.notify(notificationId, builder.build());
        }
    }
}