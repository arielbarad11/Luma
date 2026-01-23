package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.screens.simulators.BreathingSimulationActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.LogoutHelper;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button btnToExit, btnToUpdateUser, btnToPsychologists,
            btnToBreathingSimulation, btnToAdmin;
    private TextView tvHelloUser; // משתנה לברכה האישית

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> insets);

        // אתחול רכיבים
        tvHelloUser = findViewById(R.id.helloUser);
        btnToExit = findViewById(R.id.btn_main_to_exit);
        btnToUpdateUser = findViewById(R.id.tv_main_UpdateUser);
        btnToPsychologists = findViewById(R.id.btn_main_to_PsychologistList);
        btnToBreathingSimulation = findViewById(R.id.btn_main_to_BreathingSimulation);
        btnToAdmin = findViewById(R.id.btn_main_to_admin);

        // האזנה לכפתורים
        btnToExit.setOnClickListener(v -> LogoutHelper.logout(MainActivity.this));

        btnToUpdateUser.setOnClickListener(v -> startActivity(new Intent(this, UpdateUserActivity.class)));

        btnToPsychologists.setOnClickListener(v -> startActivity(new Intent(this, PsychologistListActivity.class)));

        btnToBreathingSimulation.setOnClickListener(v -> startActivity(new Intent(this, BreathingSimulationActivity.class)));

        btnToAdmin.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminActivity.class));
        });

        checkUserStatus();
    }

    private void checkUserStatus() {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) {
            redirectToLogin();
            return;
        }

        DatabaseService.getInstance().getUser(userId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User updatedUser) {
                if (updatedUser == null) {
                    redirectToLogin();
                    return;
                }
                SharedPreferencesUtil.saveUser(MainActivity.this, updatedUser);

                // עדכון הברכה האישית לפי השעה ושם המשתמש
                updateGreeting(updatedUser.getFirstName());

                // הצגת כפתור מנהל רק אם הוא אדמין
                if (updatedUser.isAdmin()) {
                    btnToAdmin.setVisibility(View.VISIBLE);
                } else {
                    btnToAdmin.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailed(Exception e) {
                redirectToLogin();
            }
        });
    }

    /**
     * פונקציה המעדכנת את ה-TextView בברכה מתאימה לפי השעה ביום
     */
    private void updateGreeting(String firstName) {
        if (tvHelloUser == null) return;

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        // חלוקת שעות מדויקת ללא חפיפה
        if (hour >= 5 && hour < 12) {
            greeting = "בוקר טוב, ";
        } else if (hour >= 12 && hour < 18) {
            greeting = "צהריים טובים, ";
        } else if (hour >= 18 && hour < 23) {
            greeting = "ערב טוב, ";
        } else {
            greeting = "לילה טוב, ";
        }

        tvHelloUser.setText(greeting + firstName);
    }

    private void redirectToLogin() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
