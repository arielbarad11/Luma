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

/**
 * SplashActivity - מסך הפתיחה של האפליקציה.
 * תפקידו: להציג לוגו לזמן קצר ולנתב את המשתמש למסך הנכון (התחברות, מסך ראשי או מסך ניהול).
 */
public class SplashActivity extends BaseActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // מאפשר תצוגה על כל המסך (כולל מתחת לסטטוס בר)
        setContentView(R.layout.activity_splash);

        // הגדרת Padding אוטומטי כדי שהתוכן לא יוסתר על ידי ה-System Bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "SplashActivity started");

        // השהייה של 3 שניות כדי להציג את מסך הפתיחה, ואז בדיקה לאן לנווט
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Starting navigation check");
            checkUserAndNavigate();
        }, 3000);
    }

    /**
     * פונקציה הבודקת את סטטוס המשתמש ומחליטה על המשך הניווט.
     */
    private void checkUserAndNavigate() {
        // 1. בדיקה האם המשתמש מחובר (מידע שנשמר ב-SharedPreferences)
        if (!SharedPreferencesUtil.isUserLoggedIn(this)) {
            Log.d(TAG, "User not logged in, navigating to Landing");
            navigateToActivity(LandingActivity.class);
            return;
        }

        // 2. שליפת נתוני המשתמש המקומיים
        User current = SharedPreferencesUtil.getUser(this);
        
        if (current == null) {
            SharedPreferencesUtil.signOutUser(this);
            navigateToActivity(LandingActivity.class);
            return;
        }

        // 3. אימות מול ה-Database - בדיקה שהמשתמש עדיין קיים ומה התפקיד שלו (אדמין או רגיל)
        DatabaseService.getInstance().getUser(current.getId(), new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                runOnUiThread(() -> {
                    if (user == null) {
                        // המשתמש לא נמצא במסד הנתונים - ננתק אותו
                        SharedPreferencesUtil.signOutUser(SplashActivity.this);
                        navigateToActivity(LandingActivity.class);
                    } else {
                        // עדכון המידע המקומי ושליחה למסך המתאים לפי סוג המשתמש
                        SharedPreferencesUtil.saveUser(SplashActivity.this, user);
                        if (user.isAdmin()) {
                            navigateToActivity(AdminActivity.class);
                        } else {
                            navigateToActivity(MainActivity.class);
                        }
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                // במקרה של שגיאת תקשורת - נחזור למסך הפתיחה/התחברות
                runOnUiThread(() -> {
                    SharedPreferencesUtil.signOutUser(SplashActivity.this);
                    navigateToActivity(LandingActivity.class);
                });
            }
        });
    }

    /**
     * פונקציית עזר למעבר בין Activities וסגירת המסך הנוכחי.
     */
    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(SplashActivity.this, activityClass);
        // FLAG_ACTIVITY_CLEAR_TASK דואג שהמשתמש לא יוכל לחזור אחורה למסך ה-Splash בלחיצה על "חזור"
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}