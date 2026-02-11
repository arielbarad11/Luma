package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

public class SplashActivity extends BaseActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "SplashActivity started");

        // Wait 3 seconds then check login status
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Starting navigation check");
            checkUserAndNavigate();
        }, 3000);
    }

    private void checkUserAndNavigate() {
        Log.d(TAG, "Checking if user is logged in");

        if (!SharedPreferencesUtil.isUserLoggedIn(this)) {
            Log.d(TAG, "User not logged in, navigating to Landing");
            navigateToActivity(LandingActivity.class);
            return;
        }

        User current = SharedPreferencesUtil.getUser(this);
        Log.d(TAG, "User logged in, ID: " + (current != null ? current.getId() : "null"));

        if (current == null) {
            Log.e(TAG, "Current user is null despite being logged in");
            SharedPreferencesUtil.signOutUser(this);
            navigateToActivity(LandingActivity.class);
            return;
        }

        Log.d(TAG, "Fetching user from database");
        DatabaseService.getInstance().getUser(current.getId(), new DatabaseService.DatabaseCallback<>() {

            @Override
            public void onCompleted(User user) {
                Log.d(TAG, "Database fetch completed. User: " + (user != null ? user.getId() : "null"));
                runOnUiThread(() -> {
                    if (user == null) {
                        Log.d(TAG, "User not found in database, signing out");
                        SharedPreferencesUtil.signOutUser(SplashActivity.this);
                        navigateToActivity(LandingActivity.class);
                    } else {
                        SharedPreferencesUtil.saveUser(SplashActivity.this, user);
                        if (user.isAdmin()) {
                            Log.d(TAG, "User is admin, navigating to Admin");
                            navigateToActivity(AdminActivity.class);
                        } else {
                            Log.d(TAG, "User is regular, navigating to Main");
                            navigateToActivity(MainActivity.class);
                        }
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Database fetch failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    SharedPreferencesUtil.signOutUser(SplashActivity.this);
                    navigateToActivity(LandingActivity.class);
                });
            }
        });
    }

    private void navigateToActivity(Class<?> activityClass) {
        Log.d(TAG, "Navigating to: " + activityClass.getSimpleName());
        try {
            Intent intent = new Intent(SplashActivity.this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Log.d(TAG, "Navigation completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Navigation failed: " + e.getMessage(), e);
        }
    }
}