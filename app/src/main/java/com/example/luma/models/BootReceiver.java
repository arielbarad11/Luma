package com.example.luma.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Device rebooted, rescheduling alarms...");
            
            String userId = SharedPreferencesUtil.getUserId(context);
            if (userId != null) {
                // נמשוך את המטרות ונתזמן אותן מחדש
                DatabaseService.getInstance().getGoals(userId, new DatabaseService.DatabaseCallback<List<Goal>>() {
                    @Override
                    public void onCompleted(List<Goal> goals) {
                        if (goals != null) {
                            // כאן צריך לקרוא למתודת התזמון. 
                            // הערה: כדאי להוציא את לוגיקת התזמון למחלקת Utility כדי שיהיה אפשר לקרוא לה מכל מקום.
                        }
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e("BootReceiver", "Failed to reschedule alarms", e);
                    }
                });
            }
        }
    }
}