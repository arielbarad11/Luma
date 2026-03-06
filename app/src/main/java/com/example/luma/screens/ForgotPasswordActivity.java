package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.Validator;

/**
 * ForgotPasswordActivity - מסך שחזור סיסמה.
 * מאפשר למשתמש לאפס את הסיסמה שלו על ידי הזנת אימייל וסיסמה חדשה.
 */
public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail, etNewPassword;
    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // הגדרת Padding למניעת חפיפה עם שולי המסך (System Bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_forgotPassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול השירות לניהול ה-Database
        databaseService = DatabaseService.getInstance();

        // קישור רכיבי ה-UI
        etEmail = findViewById(R.id.et_email);
        etNewPassword = findViewById(R.id.et_new_password);
        Button btnUpdatePassword = findViewById(R.id.btn_update_password);
        ImageButton btnGoBack = findViewById(R.id.imageButton_goBack);

        // הגדרת מאזינים ללחיצות
        if (btnGoBack != null) {
            btnGoBack.setOnClickListener(v -> finish()); // חזרה למסך הקודם
        }
        btnUpdatePassword.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_update_password) {
            updatePassword(); // תחילת תהליך עדכון הסיסמה
        }
    }

    /**
     * פונקציה המבצעת את תהליך איפוס הסיסמה.
     */
    private void updatePassword() {
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        // 1. בדיקת תקינות הקלט (וולידציה)
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("בבקשה הזן אימייל תקין");
            etEmail.requestFocus();
            return;
        }

        if (!Validator.isPasswordValid(newPassword)) {
            etNewPassword.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
            etNewPassword.requestFocus();
            return;
        }

        // 2. חיפוש המשתמש ב-Firebase לפי האימייל שהוזן
        databaseService.getUserByEmail(email, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                // אם לא נמצא משתמש עם אימייל כזה
                if (user == null) {
                    etEmail.setError("לא נמצא משתמש עם האימייל הזה");
                    etEmail.requestFocus();
                    return;
                }

                // 3. עדכון הסיסמה החדשה באמצעות טרנזקציה (עדכון בטוח)
                databaseService.updateUser(user.getId(), u -> {
                    if (u == null) return null;
                    u.setPassword(newPassword); // הגדרת הסיסמה החדשה
                    return u;
                }, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void result) {
                        Toast.makeText(ForgotPasswordActivity.this, "הסיסמה עודכנה בהצלחה!", Toast.LENGTH_SHORT).show();
                        
                        // 4. ניתוב המשתמש למסך הבית המתאים (רגיל או אדמין)
                        Intent nextIntent;
                        if (!user.isAdmin()) {
                            nextIntent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
                        } else {
                            nextIntent = new Intent(ForgotPasswordActivity.this, AdminActivity.class);
                        }
                        
                        nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(nextIntent);
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this, "שינוי הסיסמה נכשל, נסה שוב", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ForgotPasswordActivity.this, "שגיאה בשליפת נתוני משתמש", Toast.LENGTH_SHORT).show();
            }
        });
    }
}