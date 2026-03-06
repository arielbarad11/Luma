package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.example.luma.utils.Validator;

/**
 * LoginActivity - מסך התחברות למערכת.
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        
        // הגדרת ריווחים אוטומטיים למניעת חפיפה עם שולי המסך
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול רכיבי ה-UI מה-XML
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login_login);
        tvRegister = findViewById(R.id.tv_login_register);
        tvForgotPassword = findViewById(R.id.tv_login_forgotPassword);

        // הגדרת מאזינים ללחיצות
        btnLogin.setOnClickListener(this);
        tvRegister.setOnClickListener(this);
        tvForgotPassword.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnLogin.getId()) {
            // לחיצה על התחברות - איסוף נתונים וביצוע וולידציה
            String email = (etEmail.getText().toString()).trim();
            String password = (etPassword.getText().toString()).trim();

            if (!checkInput(email, password)) return;

            loginUser(email, password);
        } else if (v.getId() == tvRegister.getId()) {
            // מעבר למסך הרשמה
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        } else if (v.getId() == tvForgotPassword.getId()) {
            // מעבר למסך שחזור סיסמה
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        }
    }

    /**
     * בדיקת תקינות הקלט (אימייל תקין וסיסמה באורך המינימלי).
     */
    private boolean checkInput(String email, String password) {
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("כתובת אימייל לא תקינה");
            etEmail.requestFocus();
            return false;
        }
        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("סיסמה חייבת להכיל לפחות 6 תווים");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * פונקציה המבצעת את ההתחברות מול ה-Database.
     */
    private void loginUser(String email, String password) {
        // פנייה לשירות מסד הנתונים כדי למצוא משתמש עם אימייל וסיסמה תואמים
        DatabaseService.getInstance().getUserByEmailAndPassword(email, password, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                // המשתמש נמצא בהצלחה
                Log.d(TAG, "User logged in: " + user.getFirstName());
                
                // 1. שמירת המשתמש באופן מקומי ב-SharedPreferences (כדי שלא יצטרך להתחבר כל פעם מחדש)
                SharedPreferencesUtil.saveUser(LoginActivity.this, user);
                
                // 2. ניתוב המשתמש לפי הרשאות - מנהל או משתמש רגיל
                Intent nextIntent;
                if (user.isAdmin()) {
                    nextIntent = new Intent(LoginActivity.this, AdminActivity.class);
                } else {
                    nextIntent = new Intent(LoginActivity.this, MainActivity.class);
                }
                
                // ניקוי היסטוריית המסכים ומעבר למסך הבא
                nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(nextIntent);
            }

            @Override
            public void onFailed(Exception e) {
                // התחברות נכשלה - הצגת שגיאה למשתמש
                etPassword.setError("אימייל או סיסמה שגויים");
                etPassword.requestFocus();
                SharedPreferencesUtil.signOutUser(LoginActivity.this);
            }
        });
    }
}