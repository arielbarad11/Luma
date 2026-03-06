package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.example.luma.utils.Validator;

import java.util.ArrayList;

/**
 * RegisterActivity - מסך הרשמת משתמש חדש למערכת.
 */
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RegisterActivity";
    private EditText etEmail, etPassword, etFName, etAge;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        
        // הגדרת ריווחים אוטומטיים
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול רכיבי ה-UI
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etFName = findViewById(R.id.et_register_first_name);
        etAge = findViewById(R.id.et_register_age);
        btnRegister = findViewById(R.id.btn_register_register);
        tvLogin = findViewById(R.id.tv_register_login);

        btnRegister.setOnClickListener(this);
        tvLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRegister.getId()) {
            // לחיצה על כפתור הרשמה - איסוף ואימות נתונים
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String fName = etFName.getText().toString();
            int age;
            try {
                age = Integer.parseInt(etAge.getText().toString());
            } catch (NumberFormatException e) {
                etAge.setError("גיל לא תקין");
                return;
            }

            if (!checkInput(email, password)) return;

            registerUser(email, password, fName, age);
        } else if (v.getId() == tvLogin.getId()) {
            // חזרה למסך התחברות
            finish();
        }
    }

    /**
     * בדיקת תקינות הקלט (אימייל תקין וסיסמה).
     */
    private boolean checkInput(String email, String password) {
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("אימייל לא תקין");
            return false;
        }
        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("סיסמה חייבת להיות לפחות 6 תווים");
            return false;
        }
        return true;
    }

    /**
     * תהליך יצירת המשתמש החדש.
     */
    private void registerUser(String email, String password, String fName, int age) {
        // 1. יצירת מזהה ייחודי (UID) מה-DatabaseService
        String uid = DatabaseService.getInstance().generateUserId();

        // 2. יצירת אובייקט משתמש חדש עם רשימות ריקות למטרות וזמני משבר (ArrayList)
        User user = new User(uid, fName, email, password, age, new ArrayList<>(), new ArrayList<>(), false);
        
        // 3. בדיקה אם האימייל כבר קיים במערכת לפני היצירה
        DatabaseService.getInstance().checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean exists) {
                if (exists) {
                    Toast.makeText(RegisterActivity.this, "האימייל כבר קיים במערכת", Toast.LENGTH_SHORT).show();
                } else {
                    // האימייל פנוי - נמשיך ליצירת המשתמש
                    createUserInDatabase(user);
                }
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RegisterActivity.this, "שגיאה בבדיקת אימייל", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * שמירת המשתמש ב-Firebase.
     */
    private void createUserInDatabase(User user) {
        DatabaseService.getInstance().createNewUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                // שמירה הצליחה - נשמור מקומית ונעבור למסך הראשי
                SharedPreferencesUtil.saveUser(RegisterActivity.this, user);
                Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RegisterActivity.this, "הרשמה נכשלה, נסה שוב", Toast.LENGTH_SHORT).show();
            }
        });
    }
}