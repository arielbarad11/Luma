package com.example.luma.screens.adminPages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.screens.LoginActivity;
import com.example.luma.screens.MainActivity;
import com.example.luma.utils.LogoutHelper;
import com.example.luma.utils.SharedPreferencesUtil;

/**
 * AdminActivity - תפריט הניהול המרכזי של האפליקציה.
 * מסך זה נגיש רק למשתמשים שהוגדרו כמנהלים (Admin).
 */
public class AdminActivity extends AppCompatActivity {

    private Button toUsersList;
    private Button toAdminPsychologistList;
    private Button toMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        // הגדרת Padding למניעת חפיפה עם שולי המסך
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAdmin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // בדיקת אבטחה: מוודא שהמשתמש עדיין מחובר
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) {
            redirectToLogin();
            return;
        }

        // אתחול כפתורי הניווט של המנהל
        toUsersList = findViewById(R.id.btn_admin_go_to_usersList);
        toAdminPsychologistList = findViewById(R.id.btn_admin_go_to_adminPsychologistList);
        toMain = findViewById(R.id.btn_admin_go_to_main);
        Button btnToExit = findViewById(R.id.btn_admin_to_exit);

        // כפתור התנתקות (Exit) - משתמש ב-LogoutHelper המשותף
        btnToExit.setOnClickListener(v -> LogoutHelper.logout(AdminActivity.this));

        // ניווט לרשימת המשתמשים במערכת (צפייה ועריכה)
        toUsersList.setOnClickListener(view -> {
            Intent intent = new Intent(AdminActivity.this, UsersListActivity.class);
            startActivity(intent);
        });

        // ניווט לניהול פסיכולוגים (הוספה/מחיקה/עריכה)
        toAdminPsychologistList.setOnClickListener(view -> {
            Intent intent = new Intent(AdminActivity.this, AdminPsychologistListActivity.class);
            startActivity(intent);
        });

        // כפתור מעבר לממשק המשתמש הרגיל (MainActivity)
        toMain.setOnClickListener(view -> {
            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    /**
     * ניתוק והפניה למסך התחברות במקרה של בעיית הרשאות.
     */
    private void redirectToLogin() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}